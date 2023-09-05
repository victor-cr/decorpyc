package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyExpr}

case class ATLRawFunction(
                           override val attributes: Map[String, _],
                           override val fileName: String,
                           override val lineNum: Int,
                           expr: Option[PyExpr]
                         ) extends ATLNode with Attributes

object ATLRawFunction extends ATLNodeFactory[ATLRawFunction] {
  private val keyExpr: String = "expr"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawFunction = {
    val expr = context.transformPyExpr(attributes.get(keyExpr))

    new ATLRawFunction(attributes - keyExpr, fileName, lineNum, expr)
  }
}
