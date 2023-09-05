package com.codegans.decorpyc.ast

case class EndTranslate(
                         override val attributes: Map[String, _],
                         override val fileName: String,
                         override val lineNum: Int
                       ) extends ASTNode with Attributes

object EndTranslate extends ASTNodeFactory[EndTranslate] {
  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): EndTranslate =
    new EndTranslate(attributes, fileName, lineNum)
}
