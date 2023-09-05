package com.codegans.decorpyc.ast

case class Jump(override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                expression: Boolean,
                target: PyExpr
               ) extends ASTNode with Attributes

object Jump extends ASTNodeFactory[Jump] {
  private val keyExpression: String = "expression"
  private val keyTarget: String = "target"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Jump = {
    val expression = attributes(keyExpression).asInstanceOf[Boolean]
    val target = context.transformPyExpr(attributes(keyTarget)).get

    new Jump(attributes - keyExpression - keyTarget, fileName, lineNum, expression, target)
  }
}
