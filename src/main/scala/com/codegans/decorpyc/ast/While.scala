package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.IfCondition.ConditionType

import scala.collection.mutable.ListBuffer

case class While(override val attributes: Map[String, _],
                 override val children: List[ASTNode],
                 override val fileName: String,
                 override val lineNum: Int,
                 condition: Option[PyExpr]
                ) extends ASTNode with Attributes with ChildrenList[ASTNode]

object While extends ASTNodeFactory[While] {
  private val keyBlock: String = "block"
  private val keyCondition: String = "condition"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): While = {
    val condition = context.transformPyExpr(attributes.get(keyCondition))
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)

    new While(attributes - keyCondition - keyBlock, children, fileName, lineNum, condition)
  }
}
