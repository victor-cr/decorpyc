package com.codegans.decorpyc.file

import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets

case class ArchiveInfo(
                        name: String,
                        version: String,
                        offset: Long,
                        key: Option[Int],
                        index: ArchiveIndex
                      )

object ArchiveInfo {
  private val log: Logger = LoggerFactory.getLogger(classOf[ArchiveInfo])

  def apply(name: String, source: ByteSource): ArchiveInfo = {
    val archive = parseInfo(name, source)
    val offset = source.offset

    source.seek(archive.offset)

    val index = archive.key
      .map(key => ArchiveIndex(source.readZLib(), key))
      .getOrElse(ArchiveIndex(source.readZLib()))

    source.seek(offset)

    new ArchiveInfo(name, archive.version, archive.offset, archive.key, index)
  }

  private def parseIndex(source: ByteSource, offset: Long, key: Option[Int]): ArchiveIndex = {
    val position = source.offset

    source.seek(offset)

    val index = key
      .map(e => ArchiveIndex(source.readZLib(), e))
      .getOrElse(ArchiveIndex(source.readZLib()))

    source.seek(position)

    index
  }

  private def createInfo(name: String, source: ByteSource, version: String, offset: Long, key: Option[Int]): ArchiveInfo = {
    val index = parseIndex(source, offset, key)

    log.info("Read {} archive `{}` with obfuscation key `{}`: index at {} ({} entries)", version, name, key.map(_.toHexString).getOrElse("None"), offset, index.entries.size)

    ArchiveInfo(name, version, offset, key, index)
  }

  private def parseInfo(name: String, source: ByteSource): ArchiveInfo = {
    if (name.contains('.') && name.substring(name.lastIndexOf(".") + 1) == "rpi") {
      createInfo(name, source, "RPA-1.0", 0L, None)
    } else {
      val header = source.readLine(StandardCharsets.US_ASCII)

      val parts = header.split(' ')

      val version = parts.head

      version match {
        case "ALT-1.0" =>
          val offset = java.lang.Long.valueOf(parts(2), 16)
          val key = java.lang.Integer.valueOf(parts(1), 16)

          createInfo(name, source, version, offset, Some(key ^ 0xDABE8DF0))
        case "RPA-2.0" =>
          val offset = java.lang.Long.valueOf(parts(1), 16)

          createInfo(name, source, version, offset, None)
        case "ZiX-12A" | "ZiX-12B" =>
          throw new UnsupportedOperationException(s"Unsupported archive $version")
        case _ =>
          val offset = java.lang.Long.valueOf(parts(1), 16)
          val key = java.lang.Integer.valueOf(parts(2), 16)

          createInfo(name, source, version, offset, Some(key))
      }
    }
  }
}
