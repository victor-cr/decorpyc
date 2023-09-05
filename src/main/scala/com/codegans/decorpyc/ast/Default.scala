package com.codegans.decorpyc.ast

case class Default(override val attributes: Map[String, _],
                   override val fileName: String,
                   override val lineNum: Int,
                   prefix: Option[String],
                   variable: String,
                   code: Option[PyCode]
                  ) extends ASTNode with Attributes

object Default extends ASTNodeFactory[Default] {
  private val keyStore: String = "store"
  private val keyVarName: String = "varname"
  private val keyCode: String = "code"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Default = {
    val prefix = context.transformString(attributes.get(keyStore)).filter(_.startsWith("store.")).map(_.stripPrefix("store."))
    val variable = attributes(keyVarName).asInstanceOf[String]
    val code = context.transformPyCode(attributes.get(keyCode))

    new Default(attributes - keyStore - keyVarName - keyCode, fileName, lineNum, prefix, variable, code)
  }
}
