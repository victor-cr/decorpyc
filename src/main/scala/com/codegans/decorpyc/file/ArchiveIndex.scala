package com.codegans.decorpyc.file

import com.codegans.decorpyc.file.ArchiveIndex.Entry
import com.codegans.decorpyc.opcode.{OpcodeRoot, Pickle}
import com.codegans.decorpyc.opcode
import com.codegans.decorpyc.util.ByteSource

case class ArchiveIndex(entries: List[Entry])

object ArchiveIndex {
  case class Entry(name: String, offset: Long, length: Long)

  def apply(source: ByteSource, key: Int): ArchiveIndex = apply(Pickle(source, key))

  def apply(source: ByteSource): ArchiveIndex = apply(opcode.Pickle(source))

  def apply(root: OpcodeRoot): ArchiveIndex = {
    val list: List[Entry] = root.attributes.asInstanceOf[Map[String, List[List[_]]]].map {
      case (name, List((offset: Int) :: (length: Int) :: _ :: Nil)) => Entry(name, offset, length)
      case (name, List((offset: Long) :: (length: Long) :: _ :: Nil)) => Entry(name, offset, length)
      case (name, List((offset: BigInt) :: (length: Int) :: _ :: Nil)) => Entry(name, offset.toLong, length)
      case tuple => throw new IllegalArgumentException(tuple.toString())
    }.toList.sortBy(_.offset)

    ArchiveIndex(list)
  }
}
