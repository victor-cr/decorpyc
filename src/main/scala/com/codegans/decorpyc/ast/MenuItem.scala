package com.codegans.decorpyc.ast

case class MenuItem(override val children: List[ASTNode],
                    override val fileName: String,
                    override val lineNum: Int,
                    text: String,
                    condition: Option[PyExpr],
                    startLine: Int,
                    endLine: Int
                   ) extends ASTNode with ChildrenList[ASTNode]

object MenuItem {
  def apply(fileName: String, lastLine: Int, text: String, condition: Option[PyExpr], children: List[ASTNode]): MenuItem = {
    val lineList = children.map(_.lineNum)
    val lineNum = condition match {
      case Some(DebugPyExpr(_, `fileName`, line, _)) if line > lastLine => line
      case _ => lastLine + 1
    }
    val startLine = lineList.minOption.getOrElse(lineNum)
    val endLine = lineList.maxOption.getOrElse(lineNum)

    new MenuItem(children, fileName, lineNum, text, condition, startLine, endLine)
  }
}
