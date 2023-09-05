package com.codegans.decorpyc.file

import com.codegans.decorpyc.opcode.Pickle
import com.codegans.decorpyc.opcode
import com.codegans.decorpyc.util.ByteSource

case class ArchiveIndex(entries: List[ArchiveEntry]) {
}

case class ArchiveEntry(name: String, offset: Int, length: Int)

object ArchiveIndex {
  def apply(source: ByteSource, key: Int): ArchiveIndex = apply(Pickle(source, key).asInstanceOf[Map[String, List[List[_]]]])

  def apply(source: ByteSource): ArchiveIndex = apply(opcode.Pickle(source).asInstanceOf[Map[String, List[List[_]]]])

  def apply(map: Map[String, List[List[_]]]): ArchiveIndex = {
    val list: List[ArchiveEntry] = map.map { case (name, List(list)) => ArchiveEntry(name, list(0).asInstanceOf[Int], list(1).asInstanceOf[Int]) }.toList.sortBy(_.offset)

    ArchiveIndex(list)
  }
}
