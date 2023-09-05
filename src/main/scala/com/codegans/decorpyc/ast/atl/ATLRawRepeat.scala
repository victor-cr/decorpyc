package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyExpr}

case class ATLRawRepeat(
                         override val attributes: Map[String, _],
                         override val fileName: String,
                         override val lineNum: Int,
                         repeats: Option[PyExpr]
                       ) extends ATLNode with Attributes

object ATLRawRepeat extends ATLNodeFactory[ATLRawRepeat] {
  private val keyRepeats: String = "repeats"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawRepeat = {
    val repeats = context.transformPyExpr(attributes(keyRepeats))

    new ATLRawRepeat(attributes - keyRepeats, fileName, lineNum, repeats)
  }
}
