package com.codegans.decorpyc

import com.codegans.decorpyc.file.ArchiveInfo
import com.codegans.decorpyc.util.ByteSource

import java.nio.file.Paths
import java.util.zip.Inflater


class UnpackActor {
  def unpack(): Unit = {
    val source = ByteSource(Paths.get("."))
    val archive = ArchiveInfo(source)

    println(archive)
    source.close()
    //
    //    println(s"File size: ${file.size}")
    //    println(s"Version: $version")
    //    println(s"Length: $length")
    //    println(s"Magic Number: $magic")
    //    println(new String(file, header.length + 1, 17))
    //    println(new String(file, header.length + 18, 10))
    //
    //    val offset = header.length + 18
    //
    //    Iterator.iterate(FileInfo(file, offset)) { prev => FileInfo(file, prev.offset + prev.)}
    //
    //    Iterator.iterate(SlotInfo(file, offset)) { prev =>
    //      SlotInfo(file, prev.offset + 12)
    //    }.takeWhile { case SlotInfo(_, a, b, c, _) =>
    //      a != 0 || b != 0 || c != 0
    //    }.foreach { block =>
    //      println(block)
    //      if (block.length >= 0) {
    //        Files.write(Paths.get(s"./block-${block.id}.z"), file.slice(offset - 10 + block.start, offset - 10 + block.start + block.length))
    //        Files.write(Paths.get(s"./block-${block.id}.dat"), extract(file, offset - 10 + block.start, block.length))
    //      } else {
    //        Files.write(Paths.get(s"./block-${block.id}.z"), file.slice(offset - 10 + block.start, file.length))
    //        Files.write(Paths.get(s"./block-${block.id}.dat"), extract(file, offset - 10 + block.start, file.length - offset + 10 - block.start))
    //      }
    //    }
    //    val offset = header.length
    //    val slotId = file(offset + 0) | (file(offset + 1) << 8) | (file(offset + 2) << 16) | (file(offset + 3) << 24)
    //    val start = file(offset + 4) | (file(offset + 5) << 8) | (file(offset + 6) << 16) | (file(offset + 7) << 24)
    //    val length = file(offset + 8) | (file(offset + 9) << 8) | (file(offset + 10) << 16) | (file(offset + 11) << 24)

    //    println(s"Block #$slotId: $start ($length bytes)")
    //
  }

  private def extract(data: Array[Byte], offset: Int, len: Int): Array[Byte] = {
    val inflater = new Inflater()
    val buf = new Array[Byte](len * 10)

    inflater.setInput(data, offset, len)
    val size = inflater.inflate(buf)
    buf.slice(0, size)
  }
}
