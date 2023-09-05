package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, NodeContext, PyExpr}

case class SLFor(override val attributes: Map[String, _],
                 override val children: List[SLNode],
                 override val fileName: String,
                 override val lineNum: Int,
                 variable: String,
                 expression: Option[PyExpr],
                 keywords: Map[String, Option[PyExpr]]
                ) extends SLNode with Attributes with ChildrenList[SLNode]

object SLFor extends SLNodeFactory[SLFor] {
  private val keyKeyword: String = "keyword"
  private val keyExpression: String = "expression"
  private val keyVariable: String = "variable"
  private val keyChildren: String = "children"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLFor = {
    val keywords = attributes(keyKeyword).asInstanceOf[List[List[_]]].map {
      case (key: String) :: value :: Nil => key -> context.transformPyExpr(value)
    }.toMap
    val expression = context.transformPyExpr(attributes(keyExpression))
    val variable = attributes(keyVariable).asInstanceOf[String]
    val children = attributes(keyChildren).asInstanceOf[List[_]].flatMap(context.transformSL)

    new SLFor(attributes - keyKeyword - keyExpression - keyVariable - keyChildren, children, fileName, lineNum, variable, expression, keywords)
  }
}
