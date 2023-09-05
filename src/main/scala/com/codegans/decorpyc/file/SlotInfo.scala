package com.codegans.decorpyc.file

case class SlotInfo(id: Int, start: Int, length: Int)

object SlotInfo {
  val terminator: SlotInfo = SlotInfo(0, 0, 0)
}
