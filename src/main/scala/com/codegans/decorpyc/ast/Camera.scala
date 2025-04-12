package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.atl.ATLNode

case class Camera(override val attributes: Map[String, _],
                  override val atl: List[ATLNode],
                  override val fileName: String,
                  override val lineNum: Int,
                  layer: String,
                  atList: List[PyExpr],
                  next: Option[String]
                 ) extends ASTNode with Attributes with Transformations

object Camera extends ASTNodeFactory[Camera] {
  private val keyATL: String = "atl"
  private val keyLayer: String = "layer"
  private val keyNext: String = "next"
  private val keyAtList: String = "at_list"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Camera = {
    val atl = context.transformATL(attributes.get(keyATL))
    val layer = attributes(keyLayer).asInstanceOf[String]
    val next = attributes(keyNext).asInstanceOf[Option[String]]
    val atList = attributes(keyAtList).asInstanceOf[List[_]].flatMap(context.transformPyExpr)

    new Camera(attributes, atl, fileName, lineNum, layer, atList, next)
  }
}
