package com.codegans.decorpyc.ast

case class Scene(override val attributes: Map[String, _],
                 override val fileName: String,
                 override val lineNum: Int,
                 layer: Option[String],
                 imSpec: List[_]
                ) extends ASTNode with Attributes

object Scene extends ASTNodeFactory[Scene] {
  private val keyLayer: String = "layer"
  private val keyIMSpec: String = "imspec"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Scene = {
    val layer = context.transformString(attributes(keyLayer))
    val imspec = context.transformList(attributes(keyIMSpec))

    new Scene(attributes - keyLayer - keyIMSpec, fileName, lineNum, layer, imspec)
  }
}