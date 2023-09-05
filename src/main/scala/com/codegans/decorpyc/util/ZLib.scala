package com.codegans.decorpyc.util

import org.slf4j.{Logger, LoggerFactory}

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Inflater
import scala.collection.mutable.ListBuffer

object ZLib {
  private val ids = new AtomicLong()
  private val log: Logger = LoggerFactory.getLogger(classOf[ZLib.type])
  private val bufferSize = 1024

  def decompress(input: RandomAccessFile, offset: Int, size: Int): Array[Byte] = {
    val id = ids.incrementAndGet()
    val inflater = create()

    log.debug(s"Created file-based inflater #$id (with offset: $offset and size: $size)")
    inflater.setInput(input.getChannel.map(MapMode.READ_ONLY, offset, size))
    inflate(id, inflater)
  }

  def decompress(input: ByteBuffer): Array[Byte] = {
    val id = ids.incrementAndGet()
    val inflater = create()

    log.debug(s"Created buffer-based inflater #$id")
    inflater.setInput(input)
    inflate(id, inflater)
  }

  def decompress(data: Array[Byte], offset: Int, length: Int): Array[Byte] = {
    val id = ids.incrementAndGet()
    val inflater = create()

    log.debug(s"Created array-based inflater #$id")
    inflater.setInput(data, offset, length)
    inflate(id, inflater)
  }

  private def create(): Inflater = new Inflater()

  private def inflate(id: Long, inflater: Inflater): Array[Byte] = {
    val output = ByteBuffer.allocate(bufferSize)
    val arrays = ListBuffer[Array[Byte]]()

    while (!inflater.finished()) {
      val written = inflater.inflate(output)

      if (written > 0) {
        log.debug(s"Read $written bytes from inflater #$id")
        val buf = new Array[Byte](written)
        output.flip().get(buf, 0, written)
        arrays.addOne(buf)
      }

      output.clear()
    }

    log.debug(s"Concatenating ${arrays.size} byte-array(s) from inflater #$id")

    Array.concat(arrays.toSeq: _*)
  }
}
