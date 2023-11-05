package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.atl.ATLNode

case class ShowLayer(override val attributes: Map[String, _],
                     override val atl: List[ATLNode],
                     override val fileName: String,
                     override val lineNum: Int,
                     layer: String,
                     atList: List[PyExpr]
                    ) extends ASTNode with Attributes with Transformations

object ShowLayer extends ASTNodeFactory[ShowLayer] {
  private val keyATL: String = "atl"
  private val keyLayer: String = "layer"
  private val keyAtList: String = "at_list"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ShowLayer = {
    val atl = context.transformATL(attributes.get(keyATL))
    val layer = attributes(keyLayer).asInstanceOf[String]
    val atList = attributes(keyAtList).asInstanceOf[List[_]].flatMap(context.transformPyExpr)

    new ShowLayer(attributes - keyATL - keyLayer - keyAtList, atl, fileName, lineNum, layer, atList)
  }
}
