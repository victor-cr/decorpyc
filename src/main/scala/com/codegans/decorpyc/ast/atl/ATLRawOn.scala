package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast.{Attributes, ChildrenMap, Node}

case class ATLRawOn(
                     override val attributes: Map[String, _],
                     override val children: Map[String, ATLNode],
                     override val fileName: String,
                     override val lineNum: Int,
                   ) extends ATLNode with Attributes with ChildrenMap[ATLNode]

object ATLRawOn extends ATLNodeFactory[ATLRawOn] {
  private val keyHandlers: String = "handlers"

  override def apply(context: ast.NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawOn = {
    val children = context.transformStringMap(attributes(keyHandlers)).map { case (key, value) => key -> context.transformATL(value).head }

    new ATLRawOn(attributes - keyHandlers, children, fileName, lineNum)
  }
}
