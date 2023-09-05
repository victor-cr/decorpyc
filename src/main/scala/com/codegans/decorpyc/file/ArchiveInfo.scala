package com.codegans.decorpyc.file

import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets

case class ArchiveInfo(
                        name: String,
                        version: String,
                        offset: Long,
                        key: Long,
                        index: ArchiveIndex,
                        files: List[FileInfo]
                      )

object ArchiveInfo {
  private val log: Logger = LoggerFactory.getLogger(classOf[ArchiveInfo])

  def apply(source: ByteSource): ArchiveInfo = {
    val header = source.readLine(StandardCharsets.US_ASCII)

    val parts = header.split(' ')

    val version = parts(0)
    val start = java.lang.Long.valueOf(parts(1), 16).toInt
    val key = java.lang.Long.valueOf(parts(2), 16).toInt

    val offset = source.offset

    log.info("Read archive `{}` with obfuscation key `{}`: index at `{}`", version, key, start)

    source.seek(start)
    val index = ArchiveIndex(source.readZLib(), key)
    source.seek(offset)

    val files = index.entries.map { case ArchiveEntry(name, off, len) =>
      source.seek(off)

      FileInfo(name, source.read(len))
    }

    ArchiveInfo("<unknown yet>", version, start, key, index, files)
  }
}
