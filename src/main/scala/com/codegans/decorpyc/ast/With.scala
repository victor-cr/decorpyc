package com.codegans.decorpyc.ast

case class With(override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                expression: Option[PyExpr],
                paired: Option[PyExpr]
               ) extends ASTNode with Attributes

object With extends ASTNodeFactory[With] {
  private val keyExpr = "expr"
  private val keyPaired = "paired"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): With = {
    val expression = context.transformPyExpr(attributes(keyExpr))
    val paired = context.transformPyExpr(attributes(keyPaired))

    new With(attributes - keyExpr - keyPaired, fileName, lineNum, expression, paired)
  }
}
