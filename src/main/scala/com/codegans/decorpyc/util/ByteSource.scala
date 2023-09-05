package com.codegans.decorpyc.util

import com.codegans.decorpyc.ast.{Node, Root}

import java.io.{File, FileNotFoundException, RandomAccessFile}
import java.lang.ref.Cleaner
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.nio.charset.Charset
import java.nio.file.{Path, StandardOpenOption}
import java.nio.{ByteBuffer, ByteOrder}

trait ByteSource extends AutoCloseable {
  def offset: Int

  def remaining: Int

  def length: Int

  def seek(offset: Int): Unit

  def read(len: Int): ByteSource

  def readByte(): Byte

  def readUnsignedByte(): Int = readByte() & 0xff

  def readWord(): Int

  def readInt(): Int

  def readLong(): Long

  def readBigInt(len: Int): BigInt = {
    val bytes = read(len).toArray

    BigInt(bytes)
  }

  def readLine(): String = readLine(Charset.defaultCharset())

  def readLine(charset: String): String = readLine(Charset.forName(charset))

  def readLine(charset: Charset): String = {
    val pos = offset
    while (readByte() != 0x0A) {}
    val len = offset - pos

    seek(pos)

    readText(len, charset).stripLineEnd
  }

  def readText(length: Int): String = readText(length, Charset.defaultCharset())

  def readText(length: Int, charset: String): String = readText(length, Charset.forName(charset))

  def readText(length: Int, charset: Charset): String

  def readZLib(): ByteSource

  def readZLib(length: Int): ByteSource

  def readAST(): Root = ???

  def reset(): Unit

  override def close(): Unit

  def writeTo(file: File): Unit

  def toArray: Array[Byte]
}

object ByteSource {
  private val cleaner: Cleaner = Cleaner.create()

  def apply(data: Array[Byte]): ByteSource = apply(data, 0, data.length)

  def apply(data: Array[Byte], offset: Int, length: Int): ByteSource = apply(ByteBuffer.wrap(data, offset, length))

  def apply(data: ByteBuffer): ByteSource = new BufferedByteSource(data.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN))

  def apply(source: File): ByteSource = {
    if (!source.isFile) {
      throw new FileNotFoundException(s"Cannot find file: $source")
    }

    new FileByteSource(source)
  }

  def apply(source: Path): ByteSource = new FileByteSource(source.toFile)


  private class BufferedByteSource(data: ByteBuffer) extends ByteSource {
    override def offset: Int = data.position()

    override def remaining: Int = data.remaining()

    override def length: Int = data.limit()


    override def seek(offset: Int): Unit = data.position(offset)

    override def read(length: Int): ByteSource = {
      val bytes = new Array[Byte](length)

      data.get(bytes, 0, length)

      apply(bytes, 0, length)
    }

    override def readByte(): Byte = data.get()

    override def readWord(): Int = data.getChar

    override def readInt(): Int = data.getInt

    override def readLong(): Long = data.getLong

    override def readText(length: Int, charset: Charset): String = {
      val bytes = new Array[Byte](length)

      data.get(bytes, 0, length)

      new String(bytes, 0, length, charset)
    }

    override def readZLib(): ByteSource = apply(ByteBuffer.wrap(ZLib.decompress(data)))

    override def readZLib(length: Int): ByteSource = readZLib() // TODO: add validation

    override def reset(): Unit = data.position(0)

    override def close(): Unit = {}

    override def writeTo(file: File): Unit = {
      val channel = FileChannel.open(file.toPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

      try {
        channel.write(data)
      } finally {
        channel.close()
      }
    }

    override def toArray: Array[Byte] = {
      val bytes = new Array[Byte](length)

      data.get(bytes, 0, length)

      bytes
    }
  }

  private class FileByteSource(file: File) extends ByteSource {
    private val in: RandomAccessFile = new RandomAccessFile(file, "r")
    private val channel: FileChannel = in.getChannel

    cleaner.register(this, () => close())
    reset()

    override def offset: Int = in.getFilePointer.toInt

    override def remaining: Int = length - offset

    override def length: Int = in.length().toInt

    override def seek(offset: Int): Unit = in.seek(offset)

    override def read(length: Int): ByteSource = {
      val bytes = new Array[Byte](length)

      in.readFully(bytes, 0, length)

      apply(ByteBuffer.wrap(bytes, 0, length))
    }

    override def readByte(): Byte = in.readByte()

    override def readWord(): Int = in.readChar()

    override def readInt(): Int =
      0 | (in.readByte() & 0xFF) | ((in.readByte() & 0xFF) << 8) | ((in.readByte() & 0xFF) << 16) | ((in.readByte() & 0xFF) << 24)

    override def readLong(): Long =
      0L | (in.readInt() & 0xFFFFFFFF) | ((in.readInt() & 0xFFFFFFFF) << 32)

    override def readText(length: Int, charset: Charset): String = {
      val bytes = new Array[Byte](length)

      in.readFully(bytes, 0, length)

      new String(bytes, 0, length, charset)
    }

    override def readZLib(): ByteSource = apply(ByteBuffer.wrap(ZLib.decompress(in, offset, length - offset)))

    override def readZLib(length: Int): ByteSource = apply(ByteBuffer.wrap(ZLib.decompress(in, offset, length)))

    override def reset(): Unit = in.seek(0)

    override def close(): Unit = in.close()

    override def writeTo(file: File): Unit = {
      val outChannel = FileChannel.open(file.toPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

      try {
        outChannel.write(channel.map(MapMode.READ_ONLY, offset, length))
      } finally {
        outChannel.close()
      }
    }

    override def toArray: Array[Byte] = {
      val bytes = new Array[Byte](length)

      in.write(bytes, 0, length)

      bytes
    }
  }
}
