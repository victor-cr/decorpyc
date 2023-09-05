package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyExpr}

case class SLDefault(override val attributes: Map[String, _],
                     override val fileName: String,
                     override val lineNum: Int,
                     variable: String,
                     expression: Option[PyExpr]
                    ) extends SLNode with Attributes

object SLDefault extends SLNodeFactory[SLDefault] {
  private val keyVariable: String = "variable"
  private val keyExpression: String = "expression"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLDefault = {
    val variable = attributes(keyVariable).asInstanceOf[String]
    val expression = context.transformPyExpr(attributes.get(keyExpression))

    new SLDefault(attributes - keyVariable - keyExpression, fileName, lineNum, variable, expression)
  }
}
