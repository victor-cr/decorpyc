package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, NodeContext}

case class ATLRawBlock(
                        override val attributes: Map[String, _],
                        override val children: List[ATLNode],
                        override val fileName: String,
                        override val lineNum: Int,
                        animation: Boolean
                      ) extends ATLNode with Attributes with ChildrenList[ATLNode]

object ATLRawBlock extends ATLNodeFactory[ATLRawBlock] {
  private val keyAnimation: String = "animation"
  private val keyStatements: String = "statements"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawBlock = {
    val animation = attributes(keyAnimation).asInstanceOf[Boolean]
    val children = attributes(keyStatements).asInstanceOf[List[_]].flatMap(context.transformATL)

    new ATLRawBlock(attributes - keyAnimation - keyStatements, children, fileName, lineNum, animation)
  }
}
