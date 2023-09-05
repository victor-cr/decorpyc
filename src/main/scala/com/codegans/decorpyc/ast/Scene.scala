package com.codegans.decorpyc.ast

case class Scene(override val attributes: Map[String, _],
                 override val fileName: String,
                 override val lineNum: Int,
                 layer: Option[_],
                 imSpec: List[_]
                ) extends ASTNode with Attributes

object Scene extends ASTNodeFactory[Scene] {
  private val keyLayer: String = "layer"
  private val keyIMSpec: String = "imspec"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Scene = {
    val layer = attributes(keyLayer).asInstanceOf[Option[_]]
    val imspec = attributes(keyIMSpec).asInstanceOf[List[_]]

    new Scene(attributes - keyLayer - keyIMSpec, fileName, lineNum, layer, imspec)
  }
}