package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, NodeContext, PyExpr}

case class SLDisplayable(override val attributes: Map[String, _],
                         override val children: List[SLNode],
                         override val fileName: String,
                         override val lineNum: Int,
                         name: Option[String],
                         positional: List[Option[PyExpr]],
                         keyword: Map[String, Option[PyExpr]]
                        ) extends SLNode with Attributes with ChildrenList[SLNode]

object SLDisplayable extends SLNodeFactory[SLDisplayable] {
  private val keyName: String = "name"
  private val keyPositional: String = "positional"
  private val keyKeyword: String = "keyword"
  private val keyChildren: String = "children"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLDisplayable = {
    val name = context.transformString(attributes.get(keyName))
    val positional = attributes(keyPositional).asInstanceOf[List[_]].map(context.transformPyExpr)
    val keyword = attributes(keyKeyword).asInstanceOf[List[List[_]]].map { case (key: String) :: value :: Nil => key -> context.transformPyExpr(value) }.toMap
    val children = attributes(keyChildren).asInstanceOf[List[_]].flatMap(context.transformSL)

    new SLDisplayable(attributes - keyName - keyPositional - keyKeyword - keyChildren, children, fileName, lineNum, name, positional, keyword)
  }
}
