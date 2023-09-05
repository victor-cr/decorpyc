package com.codegans.decorpyc.ast

case class Hide(override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                imSpec: List[_]
               ) extends ASTNode with Attributes

object Hide extends ASTNodeFactory[Hide] {
  private val keyIMSpec: String = "imspec"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Hide = {
    val imspec = attributes(keyIMSpec).asInstanceOf[List[_]]

    new Hide(attributes - keyIMSpec, fileName, lineNum, imspec)
  }
}
