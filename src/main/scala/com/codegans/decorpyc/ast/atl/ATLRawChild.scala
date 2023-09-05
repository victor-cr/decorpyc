package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, NodeContext}

case class ATLRawChild(
                        override val attributes: Map[String, _],
                        override val children: List[ATLNode],
                        override val fileName: String,
                        override val lineNum: Int
                      ) extends ATLNode with Attributes with ChildrenList[ATLNode]

object ATLRawChild extends ATLNodeFactory[ATLRawChild] {
  private val keyChildren: String = "children"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawChild = {
    val children = attributes(keyChildren).asInstanceOf[List[_]].flatMap(context.transformATL)

    new ATLRawChild(attributes - keyChildren, children, fileName, lineNum)
  }
}
