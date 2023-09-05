package com.codegans.decorpyc.ast

case class Python(
                   override val attributes: Map[String, _],
                   override val fileName: String,
                   override val lineNum: Int,
                   prefix: Option[String],
                   code: Option[PyCode]
                 ) extends ASTNode with Attributes

object Python extends ASTNodeFactory[Python] {
  private val keyCode: String = "code"
  private val keyStore: String = "store"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Python = {
    val code = context.transformPyCode(attributes(keyCode))
    val prefix = context.transformString(attributes.get(keyStore)).filter(_.startsWith("store.")).map(_.stripPrefix("store."))

    new Python(attributes - keyCode - keyStore, fileName, lineNum, prefix, code)
  }
}
