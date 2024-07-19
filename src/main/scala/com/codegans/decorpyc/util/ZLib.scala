package com.codegans.decorpyc.util

import org.slf4j.{Logger, LoggerFactory}

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Inflater

object ZLib {
  private val log: Logger = LoggerFactory.getLogger(classOf[ZLib.type])
  private val ids = new AtomicLong()
  private val bufferSize = 1024

  def decompress(input: RandomAccessFile, offset: Long, size: Long): File = {
    val id = ids.incrementAndGet()
    val file = File.createTempFile("decorpyc", ".zlib.tmp")
    val inflater = create()
    val out = new BufferedOutputStream(new FileOutputStream(file), bufferSize)

    try {
      log.debug(s"Created file-based inflater #$id (with offset $offset and size $size): ${file.getAbsolutePath}")
      if (size < Int.MaxValue) {
        inflater.setInput(input.getChannel.map(MapMode.READ_ONLY, offset, size))
        inflate(id, inflater, out)
      } else {
        var vSize: Long = size
        var vOffset: Long = offset
        while (size != 0) {
          val len = Math.min(Int.MaxValue, vSize)
          inflater.setInput(input.getChannel.map(MapMode.READ_ONLY, vOffset, len))
          inflate(id, inflater, out)
          vSize -= len
          vOffset += len
        }
      }
    } finally {
      out.close()
    }

    file
  }

  def decompress(input: ByteBuffer): Array[Byte] = {
    val id = ids.incrementAndGet()
    val out = new ByteArrayOutputStream()
    val inflater = create()

    log.debug(s"Created buffer-based inflater #$id")
    inflater.setInput(input)
    inflate(id, inflater, out)

    out.toByteArray
  }

  def decompress(data: Array[Byte], offset: Int, length: Int): Array[Byte] = {
    val id = ids.incrementAndGet()
    val out = new ByteArrayOutputStream()
    val inflater = create()

    log.debug(s"Created array-based inflater #$id")
    inflater.setInput(data, offset, length)
    inflate(id, inflater, out)

    out.toByteArray
  }

  private def create(): Inflater = new Inflater()

  private def inflate(id: Long, inflater: Inflater, out: OutputStream): Unit = {
    val buf = new Array[Byte](bufferSize)

    while (!inflater.finished()) {
      val written = inflater.inflate(buf)

      if (written > 0) {
        log.debug(s"Read $written bytes from inflater #$id")
        out.write(buf, 0, written)
      }
    }
  }
}
