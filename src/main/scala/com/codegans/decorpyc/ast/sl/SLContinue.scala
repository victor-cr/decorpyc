package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyExpr}

case class SLContinue(override val attributes: Map[String, _],
                      override val fileName: String,
                      override val lineNum: Int,
                      serial: BigInt
                     ) extends SLNode with Attributes

object SLContinue extends SLNodeFactory[SLContinue] {
  private val keySerial: String = "serial"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLContinue = {
    val serial = attributes(keySerial).asInstanceOf[BigInt]

    new SLContinue(attributes - keySerial, fileName, lineNum, serial)
  }
}
