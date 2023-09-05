package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast.{Attributes, ChildrenList, PyExpr}

case class SLBlock(override val attributes: Map[String, _],
                   override val children: List[SLNode],
                   override val fileName: String,
                   override val lineNum: Int,
                   keyword: Map[String, Option[PyExpr]]
                  ) extends SLNode with Attributes with ChildrenList[SLNode]

object SLBlock extends SLNodeFactory[SLBlock] {
  private val keyKeyword: String = "keyword"
  private val keyChildren: String = "children"

  override def apply(context: ast.NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLBlock = {
    val keyword = attributes(keyKeyword).asInstanceOf[List[List[_]]].map {
      case (key: String) :: value :: Nil => key -> context.transformPyExpr(value)
    }.toMap
    val children = attributes(keyChildren).asInstanceOf[List[_]].flatMap(context.transformSL)

    new SLBlock(attributes - keyKeyword - keyChildren, children, fileName, lineNum, keyword)
  }
}
