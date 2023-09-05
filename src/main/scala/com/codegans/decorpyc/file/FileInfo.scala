package com.codegans.decorpyc.file

import com.codegans.decorpyc.format.Printer
import com.codegans.decorpyc.opcode.Pickle
import com.codegans.decorpyc.transform.{MultilineSayAspect, NodeInterceptor, OpcodeTransformer}
import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

trait FileInfo {
  def name: String

  def compiled: ByteSource

  def decompiled: ByteSource
}

object FileInfo {
  private val log: Logger = LoggerFactory.getLogger(classOf[ArchiveInfo])
  private val header = "RENPY RPC2"
  private val len = header.length

  def apply(name: String, source: ByteSource): FileInfo = {
    //    val off = offset + len + originLen
    //    val origin = new String(bytes, offset, originLen, "Latin1")
    val marker = source.readText(len)

    if (marker != header) {
      log.info("Read resource raw data `{}`", name)
      source.reset()
      ResourceFileInfo(name, source)
    } else {
      log.info("Read Ren'Py compiled data `{}`", name)
      val slots = Iterator.continually(SlotInfo(source.readInt(), source.readInt(), source.readInt())).takeWhile(_ != SlotInfo.terminator).toVector

      // Write -->
      //      slots.foreach { slot =>
      //        val file = new File("target", name.stripSuffix(".rpyc") + "-" + slot.id + ".rpy")
      //
      //        file.getParentFile.mkdirs()
      //
      //        log.info("Read `{}` slot #{}@{}:{}", name, slot.id, slot.start, slot.length)
      //        source.seek(slot.start)
      //        val zlib = source.readZLib(slot.length)
      //        try {
      //          val root = Pickle.optimize(Pickle(key, zlib))
      //          Printer.write(file, root)
      //        } catch {
      //          case NonFatal(e) =>
      //            log.error("Cannot process file: {}", file, e)
      //            throw e //Ignore
      //        }
      //      }
      // Write <--

      source.seek(slots.head.start)

      val compiled = source.readZLib(slots.head.length)
      val root = Pickle(compiled)
      val decompiled = Printer.toSource(new OpcodeTransformer(NodeInterceptor(
        MultilineSayAspect.Body,
        MultilineSayAspect.AST
      )).apply(root))

      source.reset()
      RenpyFileInfo(name, compiled, decompiled, slots)
    }
  }

  private case class ResourceFileInfo(
                                       override val name: String,
                                       private val source: ByteSource
                                     ) extends FileInfo {
    override def compiled: ByteSource = source

    override def decompiled: ByteSource = source
  }

  private case class RenpyFileInfo(
                                    override val name: String,
                                    override val compiled: ByteSource,
                                    override val decompiled: ByteSource,
                                    slots: Vector[SlotInfo]
                                  ) extends FileInfo {

  }
}
