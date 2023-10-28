package com.codegans.decorpyc.util

import com.codegans.decorpyc.ast.Root

import java.io.{File, FileNotFoundException, RandomAccessFile}
import java.lang.ref.Cleaner
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.nio.charset.Charset
import java.nio.file.{Path, StandardOpenOption}
import java.nio.{ByteBuffer, ByteOrder}

trait ByteSource extends AutoCloseable {
  def offset: Long

  def remaining: Long

  def length: Long

  def seek(offset: Long): Unit

  def read(len: Long): ByteSource

  def readByte(): Byte

  def readUnsignedByte(): Int = readByte() & 0xff

  def readWord(): Int

  def readInt(): Int

  def readLong(): Long

  def readBigInt(len: Int): BigInt = {
    val bytes = read(len).toArray.reverse

    BigInt(bytes)
  }

  def readLine(): String = readLine(Charset.defaultCharset())

  def readLine(charset: String): String = readLine(Charset.forName(charset))

  def readLine(charset: Charset): String = {
    val pos = offset
    while (readByte() != 0x0A) {}
    val len = (offset - pos).toInt

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

  def apply(source: File): ByteSource = apply(source, false)

  def apply(source: Path): ByteSource = apply(source.toFile, false)

  private def apply(source: File, deleteOnExit: Boolean): ByteSource = {
    if (!source.isFile) {
      throw new FileNotFoundException(s"Cannot find file: $source")
    }

    new FileByteSource(source, deleteOnExit)
  }


  private class BufferedByteSource(data: ByteBuffer) extends ByteSource {
    override def offset: Long = data.position()

    override def remaining: Long = data.remaining()

    override def length: Long = data.limit()


    override def seek(offset: Long): Unit = data.position(offset.toInt)

    override def read(length: Long): ByteSource = {
      val len = length.toInt
      val bytes = new Array[Byte](len)

      data.get(bytes, 0, len)

      apply(bytes, 0, len)
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
      val len = length.toInt
      val bytes = new Array[Byte](len)

      data.get(bytes, 0, len)

      bytes
    }
  }

  private class FileByteSource(file: File, deleteOnExit: Boolean) extends ByteSource {
    private val in: RandomAccessFile = new RandomAccessFile(file, "r")
    private val channel: FileChannel = in.getChannel

    if (deleteOnExit) file.deleteOnExit()
    cleaner.register(this, () => close())
    reset()

    override def offset: Long = in.getFilePointer

    override def remaining: Long = length - offset

    override def length: Long = in.length()

    override def seek(offset: Long): Unit = in.seek(offset)

    override def read(length: Long): ByteSource = {
      val len = length.toInt
      val bytes = new Array[Byte](len)

      in.readFully(bytes, 0, len)

      apply(ByteBuffer.wrap(bytes, 0, len))
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

    override def readZLib(): ByteSource = apply(ZLib.decompress(in, offset, length - offset), true)

    override def readZLib(length: Int): ByteSource = apply(ZLib.decompress(in, offset, length), true)

    override def reset(): Unit = in.seek(0)

    override def close(): Unit = {
      in.close()
      if (deleteOnExit) file.delete()
    }

    override def writeTo(file: File): Unit = {
      val outChannel = FileChannel.open(file.toPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

      try {
        outChannel.write(channel.map(MapMode.READ_ONLY, offset, length))
      } finally {
        outChannel.close()
      }
    }

    override def toArray: Array[Byte] = {
      val len = length.toInt
      val bytes = new Array[Byte](len)

      in.write(bytes, 0, len)

      bytes
    }
  }
}
