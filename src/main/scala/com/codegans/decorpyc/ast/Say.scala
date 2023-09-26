package com.codegans.decorpyc.ast

case class Say(
                override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                what: String,
                interact: Boolean,
                whoFast: Boolean,
                who: Option[String],
                withA: Option[PyExpr],
                args: Option[ArgumentInfo],
                attrs: List[String],
                tempAttrs: List[String],
                multiline: Boolean,
                referenced: Boolean
              ) extends ASTNode with Attributes

object Say extends ASTNodeFactory[Say] {
  private val keyWithA: String = "with_"
  private val keyWho: String = "who"
  private val keyAttributes: String = "attributes"
  private val keyTemporaryAttributes: String = "temporary_attributes"
  private val keyArguments: String = "arguments"
  private val keyWhat: String = "what"
  private val keyInteract: String = "interact"
  private val keyWhoFast: String = "who_fast"
  private val keyStatementStart: String = "statement_start"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Say = {
    val withA = context.transformPyExpr(attributes(keyWithA))
    val who = context.transformString(attributes.get(keyWho))
    val attrs = context.transformStringList(attributes.get(keyAttributes))
    val tempAttrs = context.transformStringList(attributes.get(keyTemporaryAttributes))
    val args = context.transformArgumentInfo(attributes.get(keyArguments))
    val what = attributes(keyWhat).asInstanceOf[String]
    val interact = attributes(keyInteract).asInstanceOf[Boolean]
    val whoFast = attributes(keyWhoFast).asInstanceOf[Boolean]
    val referenced = attributes.contains(keyStatementStart)

    new Say(attributes - keyWithA - keyWho - keyAttributes - keyTemporaryAttributes - keyArguments - keyWhat - keyInteract - keyWhoFast, fileName, lineNum, what, interact, whoFast, who, withA, args, attrs, tempAttrs, multiline = false, referenced)
  }
}
