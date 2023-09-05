package com.codegans.decorpyc.ast

case class Define(
                   override val attributes: Map[String, _],
                   override val fileName: String,
                   override val lineNum: Int,
                   prefix: Option[String],
                   variable: String,
                   operator: String,
                   index: Option[_],
                   code: Option[PyCode]
                 ) extends ASTNode with Attributes

object Define extends ASTNodeFactory[Define] {
  private val keyIndex: String = "index"
  private val keyStore: String = "store"
  private val keyVarName: String = "varname"
  private val keyOperator: String = "operator"
  private val keyCode: String = "code"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Define = {
    val index = attributes.get(keyIndex)
    val prefix = context.transformString(attributes.get(keyStore)).filter(_.startsWith("store.")).map(_.stripPrefix("store."))
    val variable = attributes(keyVarName).asInstanceOf[String]
    val operator = attributes.getOrElse(keyOperator, "=").asInstanceOf[String]
    val code = context.transformPyCode(attributes(keyCode))

    new Define(attributes - keyIndex - keyStore - keyVarName - keyOperator - keyCode, fileName, lineNum, prefix, variable, operator, index, code)
  }
}
