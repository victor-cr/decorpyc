package com.codegans.decorpyc.opcode

import com.codegans.decorpyc.opcode.Pickle._
import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.{Charset, StandardCharsets}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class Pickle(key: Option[Int], _source: ByteSource) {
  private var instanceId: Int = 0
  private var proto: Option[Int] = None
  private val frames: ListBuffer[ByteSource] = ListBuffer()
  private val stack: mutable.Stack[Any] = mutable.Stack()
  private val memo: ListBuffer[Any] = ListBuffer()

  import com.codegans.decorpyc.opcode.Pickle.SizeOf.SizeOf

  def source: ByteSource = frames.lastOption.getOrElse(_source)

  @tailrec
  private def read(): Pickle = {
    if (source.remaining != 0) {
      val offset = source.offset
      val opcode = source.readByte()

      val parser = opcodeTable.getOrElse(opcode, {
        log.error("[{}] Unknown opcode 0x{}", format(offset, 8), format(opcode & 0xff, 2))
        throw new IllegalArgumentException("Unknown opcode: " + opcode)
      })

      parser.execute(this, proto).get // TODO: If exception has to be hidden modify here
      read()
    } else {
      log.info("End-of-input has been reached")
      this
    }
  }

  private def readProto(): Unit = {
    val value = source.readByte()
    if (value < minProto || value > maxProto) throw new IllegalArgumentException(s"Unsupported protocol version v$proto")
    proto = Some(value)
    log.info("Use serialization protocol v{}", value)
  }

  private def readStop(): Unit = if (stack.size != 1) {
    throw new IllegalArgumentException("Invalid stack size after parsing. Expected to have single element, but found: " + stack.size)
  }

  private def readMemoize(): Unit = memo.addOne(stack.head)

  private def readAppend(single: Boolean): Unit = {
    val value: List[Any] = if (single) List(stack.pop()) else Iterator.continually(stack.pop()).takeWhile(_ != OpcodeMarkObject).toList.reverse
    val list: List[Any] = stack.pop().asInstanceOf[List[_]] ++ value
    stack.push(list)
  }

  private def readBuild(): Unit = {
    val value = stack.pop() match {
      case None :: (map: Map[_, _]) :: Nil => SetUpdate(stack.pop(), map)
      case list: List[_] => SetState(stack.pop(), list)
      case map: Map[_, _] => SetUpdate(stack.pop(), map)
    }
    stack.push(value)
  }

  private def readFrame(): Unit = {
    val len = source.readLong()
    val frame = source.read(len.toInt) // TODO: Unsafe
    frames.addOne(frame)
  }

  private def readSetDictionary(): Unit = {
    val list = ListBuffer[(Any, Any)]()

    while (stack.head != OpcodeMarkObject) {
      val value: Any = stack.pop()
      val key: Any = stack.pop()
      list.insert(0, key -> value)
    }

    stack.pop() // markobject
    val map = list.toMap
    val parent = stack.pop()

    if (parent == Map()) {
      stack.push(map)
    } else {
      stack.push(MapInstanceWithData(parent, map))
    }
  }

  private def readSetPair(): Unit = {
    val value: Any = stack.pop()
    val key: Any = stack.pop()
    val parent = stack.pop()

    stack.push(SetMapItem(parent, key, value))
  }

  private def readTuple(len: Int): Unit = {
    val tuple: List[_] = if (len == 0) {
      Iterator.continually(stack.pop()).takeWhile(_ != OpcodeMarkObject).toList.reverse
    } else {
      Iterator.fill(len)(stack.pop()).toList.reverse
    }
    stack.push(tuple)
  }

  private def readBigInt(sizeOf: SizeOf): Unit = {
    val len = length(sizeOf)
    val value = source.readBigInt(len)
    //    val decoded = key.map(value ^ _).getOrElse(value)
    stack.push(value)
  }

  private def readInt(sizeOf: SizeOf): Unit = stack.push(length(sizeOf))

  private def readSignedInt32(): Unit = {
    val value = source.readInt()
    val decoded = key.map(value ^ _).getOrElse(value)
    stack.push(decoded)
  }

  private def readSpecial(value: Any): Unit = stack.push(value)

  private def readReduce(): Unit = {
    val args = stack.pop().asInstanceOf[List[_]]
    val ref = stack.pop()
    stack.push(Invocation(ref, args))
  }

  private def readGetMemoByIndex(sizeOf: SizeOf): Unit = stack.push(memo(length(sizeOf)))

  private def readPutMemoByIndex(sizeOf: SizeOf): Unit = {
    val index = length(sizeOf)
    val value = stack.head

    if (memo.size >= index) {
      memo.insert(index, value)
    } else {
      Iterator.range(memo.size, index).foreach(_ => memo.addOne(None))
      memo.insert(index, value)
    }
  }

  private def readShortString(charset: Charset): Unit = {
    val len = source.readByte()
    val value = source.readText(len, charset)
    stack.push(value)
  }

  private def readString(): Unit = {
    val len = source.readInt()
    val value = source.readText(len, StandardCharsets.UTF_8)
    stack.push(value)
  }

  private def readShortBytes(): Unit = {
    val len = source.readByte()
    val value = source.read(len).toArray
    stack.push(value)
  }

  private def readGlobal(): Unit = {
    val className = source.readLine(StandardCharsets.UTF_8)
    val methodName = source.readLine(StandardCharsets.UTF_8)
    stack.push(GlobalFunction(className, methodName))
  }

  private def readNewInstance(): Unit = {
    val args = stack.pop().asInstanceOf[Product].productIterator.toList
    val classRef = stack.pop()
    stack.push(NewInstance(instanceId, classRef, args))
    instanceId += 1
  }

  private def length(sizeOf: SizeOf): Int = sizeOf match {
    case SizeOf.UINT8 => source.readUnsignedByte()
    case SizeOf.UINT16 => source.readWord()
    case SizeOf.INT32 => source.readInt()
  }
}

object Pickle {
  private type Operation = Function[Pickle, Unit]

  private val log: Logger = LoggerFactory.getLogger(classOf[Pickle])
  private val minProto = 1
  private val maxProto = 5

  def apply(source: ByteSource, maybeKey: Option[Int]): OpcodeRoot = {
    val stack = new Pickle(maybeKey, source).read().stack

    stack.pop() match {
      case (attributes: Map[String, _]) :: (list: List[_]) :: Nil => OpcodeRoot(attributes, list)
      case index: Map[String, _] => OpcodeRoot(index, Nil)
      case value =>
        throw new IllegalArgumentException(s"Unexpected root format: $value")
    }
  }

  def apply(source: ByteSource, key: Int): OpcodeRoot = apply(source, Some(key))

  def apply(source: ByteSource): OpcodeRoot = apply(source, None)


  private object SizeOf extends Enumeration {
    type SizeOf = Value
    val UINT8, UINT16, INT32 = Value
  }

  private val opcodeTable: Map[Byte, OpcodeParser] = List(
    OpcodeParser("MARK", 0x28, _.readSpecial(OpcodeMarkObject), 1, "push special `markobject` on stack"),
    OpcodeParser("EMPTY_TUPLE", 0x29, _.readSpecial(Nil), 1, "push empty tuple"),
    OpcodeParser("STOP", 0x2e, _.readStop(), 1, "every pickle ends with STOP"),

    OpcodeParser("POP", 0x30, NOT_IMPLEMENTED, 1, "discard topmost stack item"),
    OpcodeParser("POP_MARK", 0x31, NOT_IMPLEMENTED, 1, "discard stack top through topmost markobject"),
    OpcodeParser("DUP", 0x32, NOT_IMPLEMENTED, 1, "duplicate top stack item"),

    OpcodeParser("BINBYTES", 0x42, NOT_IMPLEMENTED, 3, "push bytes; counted binary string argument"),
    OpcodeParser("SHORT_BINBYTES", 0x43, _.readShortBytes(), 3, "push bytes; counted binary string argument < 256 bytes"),
    OpcodeParser("FLOAT", 0x46, NOT_IMPLEMENTED, 1, "push float object; decimal string argument"),
    OpcodeParser("BINFLOAT", 0x47, NOT_IMPLEMENTED, 1, "push float; arg is 8-byte float encoding"),
    OpcodeParser("INT", 0x49, NOT_IMPLEMENTED, 1, "push integer or bool; decimal string argument"),
    OpcodeParser("BININT", 0x4a, _.readSignedInt32(), 1, "push four-byte signed int"),
    OpcodeParser("BININT1", 0x4b, _.readInt(SizeOf.UINT8), 1, "push 1-byte unsigned int"),
    OpcodeParser("LONG", 0x4c, NOT_IMPLEMENTED, 1, "push long; decimal string argument"),
    OpcodeParser("BININT2", 0x4d, _.readInt(SizeOf.UINT16), 1, "push 2-byte unsigned int"),
    OpcodeParser("NONE", 0x4e, _.readSpecial(None), 1, "push None"),

    OpcodeParser("PERSID", 0x50, NOT_IMPLEMENTED, 1, "push persistent object; id is taken from string arg"),
    OpcodeParser("BINPERSID", 0x51, NOT_IMPLEMENTED, 1, "push persistent object; id is taken from stack"),
    OpcodeParser("REDUCE", 0x52, _.readReduce(), 1, "apply callable to argtuple, both on stack"),
    OpcodeParser("STRING", 0x53, NOT_IMPLEMENTED, 1, "push string; NL-terminated string argument"),
    OpcodeParser("BINSTRING", 0x54, NOT_IMPLEMENTED, 1, "push string; counted binary string argument"),
    OpcodeParser("SHORT_BINSTRING", 0x55, _.readShortString(StandardCharsets.US_ASCII), 1, "push string; counted binary string argument < 256 bytes"),
    OpcodeParser("UNICODE", 0x56, NOT_IMPLEMENTED, 1, "push Unicode string; raw-unicode-escaped'd argument"),
    OpcodeParser("BINUNICODE", 0x58, _.readString(), 1, "push Unicode string; counted UTF-8 string argument"),
    OpcodeParser("EMPTY_LIST", 0x5d, _.readSpecial(List()), 1, "push empty list"),

    OpcodeParser("APPEND", 0x61, _.readAppend(true), 1, "append stack top to list below it"),
    OpcodeParser("BUILD", 0x62, _.readBuild(), 1, "call __setstate__ or __dict__.update()"),
    OpcodeParser("GLOBAL", 0x63, _.readGlobal(), 1, "push self.find_class(modname, name); 2 string args"),
    OpcodeParser("DICT", 0x64, NOT_IMPLEMENTED, 1, "build a dict from stack items"),
    OpcodeParser("APPENDS", 0x65, _.readAppend(false), 1, "extend list on stack by topmost stack slice"),
    OpcodeParser("GET", 0x67, NOT_IMPLEMENTED, 1, "push item from memo on stack; index is string arg"),
    OpcodeParser("BINGET", 0x68, _.readGetMemoByIndex(SizeOf.UINT8), 1, "push item from memo on stack; index is 1-byte arg"),
    OpcodeParser("INST", 0x69, NOT_IMPLEMENTED, 1, "build & push class instance"),
    OpcodeParser("LONG_BINGET", 0x6a, _.readGetMemoByIndex(SizeOf.INT32), 1, "push item from memo on stack; index is 4-byte arg"),
    OpcodeParser("LIST", 0x6c, NOT_IMPLEMENTED, 1, "build list from topmost stack items"),
    OpcodeParser("OBJ", 0x6f, NOT_IMPLEMENTED, 1, "build & push class instance"),

    OpcodeParser("PUT", 0x70, NOT_IMPLEMENTED, 1, "store stack top in memo; index is string arg"),
    OpcodeParser("BINPUT", 0x71, _.readPutMemoByIndex(SizeOf.UINT8), 1, "store stack top in memo; index is 1-byte arg"),
    OpcodeParser("LONG_BINPUT", 0x72, _.readPutMemoByIndex(SizeOf.INT32), 1, "store stack top in memo; index is 4-byte arg"),
    OpcodeParser("SETITEM", 0x73, _.readSetPair(), 1, "add key+value pair to dict"),
    OpcodeParser("TUPLE", 0x74, _.readTuple(0), 1, "build tuple from topmost stack items"),
    OpcodeParser("SETITEMS", 0x75, _.readSetDictionary(), 1, "modify dict by adding topmost key+value pairs"),
    OpcodeParser("EMPTY_DICT", 0x7d, _.readSpecial(Map()), 1, "push empty dict"),

    OpcodeParser("PROTO", 0x80, _.readProto(), 2, "identify pickle protocol"),
    OpcodeParser("NEWOBJ", 0x81, _.readNewInstance(), 2, "build object by applying cls.__new__ to argtuple"),
    OpcodeParser("EXT1", 0x82, NOT_IMPLEMENTED, 2, "push object from extension registry; 1-byte index"),
    OpcodeParser("EXT2", 0x83, NOT_IMPLEMENTED, 2, "ditto, but 2-byte index"),
    OpcodeParser("EXT4", 0x84, NOT_IMPLEMENTED, 2, "ditto, but 4-byte index"),
    OpcodeParser("TUPLE1", 0x85, _.readTuple(1), 2, "build 1-tuple from stack top"),
    OpcodeParser("TUPLE2", 0x86, _.readTuple(2), 2, "build 2-tuple from two topmost stack items"),
    OpcodeParser("TUPLE3", 0x87, _.readTuple(3), 2, "build 3-tuple from three topmost stack items"),
    OpcodeParser("NEWTRUE", 0x88, _.readSpecial(true), 2, "push True"),
    OpcodeParser("NEWFALSE", 0x89, _.readSpecial(false), 2, "push False"),
    OpcodeParser("LONG1", 0x8a, _.readBigInt(SizeOf.UINT8), 2, "push long from < 256 bytes"),
    OpcodeParser("LONG4", 0x8b, _.readBigInt(SizeOf.INT32), 2, "push really big long"),
    OpcodeParser("SHORT_BINUNICODE", 0x8c, _.readShortString(StandardCharsets.UTF_8), 4, "push short string; UTF-8 length < 256 bytes"),
    OpcodeParser("BINUNICODE8", 0x8d, NOT_IMPLEMENTED, 4, "push very long string"),
    OpcodeParser("BINBYTES8", 0x8e, NOT_IMPLEMENTED, 4, "push very long bytes string"),
    OpcodeParser("EMPTY_SET", 0x8f, NOT_IMPLEMENTED, 4, "push empty set on the stack"),

    OpcodeParser("ADDITEMS", 0x90, NOT_IMPLEMENTED, 4, "modify set by adding topmost stack items"),
    OpcodeParser("FROZENSET", 0x91, NOT_IMPLEMENTED, 4, "build frozenset from topmost stack items"),
    OpcodeParser("NEWOBJ_EX", 0x92, NOT_IMPLEMENTED, 4, "like NEWOBJ but work with keyword only arguments"),
    OpcodeParser("STACK_GLOBAL", 0x93, NOT_IMPLEMENTED, 4, "same as GLOBAL but using names on the stacks"),
    OpcodeParser("MEMOIZE", 0x94, _.readMemoize(), 4, "store top of the stack in memo"),
    OpcodeParser("FRAME", 0x95, _.readFrame(), 4, "indicate the beginning of a new frame"),
    OpcodeParser("BYTEARRAY8", 0x96, NOT_IMPLEMENTED, 5, "push bytearray"),
    OpcodeParser("NEXT_BUFFER", 0x97, NOT_IMPLEMENTED, 5, "push next out-of-band buffer"),
    OpcodeParser("READONLY_BUFFER", 0x98, NOT_IMPLEMENTED, 5, "make top of stack readonly"),
  ).map(e => e.code -> e).toMap

  private def format(value: Int, digits: Int): String = {
    val hex = value.toHexString

    if (digits > hex.length) {
      val prefix = Iterator.fill(digits - hex.length)('0').mkString
      prefix + hex
    } else {
      hex
    }
  }

  private def NOT_IMPLEMENTED: Operation = _ => throw new NotImplementedError

  private case class OpcodeParser(name: String, code: Byte, operation: Operation, protocol: Byte, description: String) {
    def execute(pickle: Pickle, maybeProto: Option[Int]): Try[Unit] = {

      try {
        if (maybeProto.exists(_ < protocol)) {
          throw new IllegalArgumentException(s"Incompatible protocol version $protocol")
        }

        operation(pickle)
        log.debug("Executed opcode 0x{} [{}] parser: {}", format(code, 2), name, description)
        Success()
      } catch {
        case NonFatal(e) =>
          log.error("Failed during execution of opcode 0x{} [{}] processor: {}", format(code, 2), name, description, e)
          Failure(e)
      }
    }
  }

  private object OpcodeParser {
    def apply(name: String, code: Int, operation: Operation, protocol: Int, description: String): OpcodeParser =
      new OpcodeParser(name, code.toByte, operation, protocol.toByte, description)
  }

  //  val table: Map[Byte, Operation] = Map(
  //    bMARK -> (_.readSpecial(markObject)),
  //    bSTOP -> (_.readStop()),
  //    bBININT -> (_.readSignedInt32()),
  //    bBININT1 -> (_.readUnsignedInt8()),
  //    bBININT2 -> (_.readUnsignedInt16()),
  //    bNONE -> (_.readSpecial(None)),
  //    bREDUCE -> (_.readReduce()),
  //    bSHORT_BINSTRING -> (_.readShortString(StandardCharsets.US_ASCII)),
  //    bBINUNICODE -> (_.readString()),
  //    bAPPEND -> (_.readAppend(true)),
  //    bBUILD -> (_.readBuild()),
  //    bGLOBAL -> (_.readGlobal()),
  //    bEMPTY_DICT -> (_.readEmptyDictionary()),
  //    bAPPENDS -> (_.readAppend(false)),
  //    bEMPTY_LIST -> (_.readEmptyList()),
  //    bBINGET -> (_.readGetMemoByIndex(true)),
  //    bLONG_BINGET -> (_.readGetMemoByIndex(false)),
  //    bBINPUT -> (_.readPutMemoByIndex(true)),
  //    bLONG_BINPUT -> (_.readPutMemoByIndex(false)),
  //    bSETITEM -> (_.readSetPair()),
  //    bTUPLE -> (_.readTuple(-1)),
  //    bEMPTY_TUPLE -> (_.readTuple(0)),
  //    bSETITEMS -> (_.readSetDictionary()),
  //
  //    bPROTO -> (_.readProto()),
  //    bNEWOBJ -> (_.readNewInstance()),
  //    bTUPLE1 -> (_.readTuple(1)),
  //    bTUPLE2 -> (_.readTuple(2)),
  //    bTUPLE3 -> (_.readTuple(3)),
  //    bNEWTRUE -> (_.readSpecial(true)),
  //    bNEWFALSE -> (_.readSpecial(false)),
  //    bLONG1 -> (_.readBigInt(true)),
  //    bLONG4 -> (_.readBigInt(false)),
  //
  //    bSHORT_BINBYTES -> (_.readShortBytes()),
  //
  //    bSHORT_BINUNICODE -> (_.readShortString(StandardCharsets.UTF_8)),
  //    bMEMOIZE -> (_.readMemoize()),
  //    bFRAME -> (_.readFrame())
  //  )
}
