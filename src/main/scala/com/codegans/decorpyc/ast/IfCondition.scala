package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.IfCondition.ConditionType.ConditionType

case class IfCondition(override val children: List[ASTNode],
                       override val fileName: String,
                       override val lineNum: Int,
                       conditionType: ConditionType,
                       condition: Option[PyExpr],
                       startLine: Int,
                       endLine: Int) extends ASTNode with ChildrenList[ASTNode]

object IfCondition {

  def apply(fileName: String, lastLine: Int, conditionType: ConditionType, condition: Option[PyExpr], children: List[ASTNode]): IfCondition = {
    val lineList = children.map(_.lineNum)
    val lineNum = condition match {
      case Some(DebugPyExpr(_, `fileName`, line, _)) if line >= lastLine => line
      case _ => lastLine + 1
    }
    val startLine = lineList.minOption.getOrElse(lineNum)
    val endLine = lineList.maxOption.getOrElse(lineNum)

    new IfCondition(children, fileName, lineNum, conditionType, condition, startLine, endLine)
  }

  object ConditionType extends Enumeration {
    type ConditionType = Value

    val IF, ELIF, ELSE = Value
  }
}
