package com.codegans.decorpyc.ast

case class Call(override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                label: PyExpr,
                expression: Boolean,
                args: Option[ArgumentInfo]
               ) extends ASTNode with Attributes

object Call extends ASTNodeFactory[Call] {
  private val keyLabel: String = "label"
  private val keyExpression: String = "expression"
  private val keyArguments: String = "arguments"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Call = {
    val label = context.transformPyExpr(attributes(keyLabel)).get
    val expression = attributes(keyExpression).asInstanceOf[Boolean]
    val args = context.transformArgumentInfo(attributes.get(keyArguments))

    new Call(attributes - keyLabel - keyExpression - keyArguments, fileName, lineNum, label, expression, args)
  }
}
