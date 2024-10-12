package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast.{Attributes, ChildrenMap, PyExpr}

case class ATLRawTime(
                       override val attributes: Map[String, _],
                       override val fileName: String,
                       override val lineNum: Int,
                       time: Option[PyExpr]
                     ) extends ATLNode with Attributes

object ATLRawTime extends ATLNodeFactory[ATLRawTime] {
  private val keyTime: String = "time"

  override def apply(context: ast.NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawTime = {
    val time = context.transformPyExpr(attributes(keyTime))

    new ATLRawTime(attributes - keyTime, fileName, lineNum, time)
  }
}
