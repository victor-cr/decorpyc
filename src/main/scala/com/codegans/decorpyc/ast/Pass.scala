package com.codegans.decorpyc.ast

case class Pass(
                 override val attributes: Map[String, _],
                 override val fileName: String,
                 override val lineNum: Int
               ) extends ASTNode with Attributes

object Pass extends ASTNodeFactory[Pass] {
  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Pass =
    new Pass(attributes, fileName, lineNum)
}
