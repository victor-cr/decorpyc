package com.codegans.decorpyc.ast

case class Return(override val attributes: Map[String, _],
                  override val fileName: String,
                  override val lineNum: Int,
                  expression: Option[PyExpr]
                 ) extends ASTNode with Attributes

object Return extends ASTNodeFactory[Return] {
  private val keyExpression: String = "expression"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Return = {
    val expression = context.transformPyExpr(attributes.get(keyExpression))

    new Return(attributes - keyExpression, fileName, lineNum, expression)
  }
}