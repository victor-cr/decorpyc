package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, NodeContext}

case class ATLRawParallel(
                           override val attributes: Map[String, _],
                           override val children: List[ATLNode],
                           override val fileName: String,
                           override val lineNum: Int
                         ) extends ATLNode with Attributes with ChildrenList[ATLNode]

object ATLRawParallel extends ATLNodeFactory[ATLRawParallel] {
  private val keyBlocks: String = "blocks"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawParallel = {
    val children = attributes(keyBlocks).asInstanceOf[List[_]].flatMap(context.transformATL)

    new ATLRawParallel(attributes - keyBlocks, children, fileName, lineNum)
  }
}
