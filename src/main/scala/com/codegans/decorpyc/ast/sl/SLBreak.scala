package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, NodeContext}

case class SLBreak(override val attributes: Map[String, _],
                   override val fileName: String,
                   override val lineNum: Int,
                   serial: BigInt
                  ) extends SLNode with Attributes

object SLBreak extends SLNodeFactory[SLBreak] {
  private val keySerial: String = "serial"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLBreak = {
    val serial = attributes(keySerial).asInstanceOf[BigInt]

    new SLBreak(attributes - keySerial, fileName, lineNum, serial)
  }
}
