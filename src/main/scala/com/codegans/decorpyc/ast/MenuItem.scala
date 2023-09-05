package com.codegans.decorpyc.ast

case class MenuItem(override val children: List[ASTNode],
                    override val fileName: String,
                    override val lineNum: Int,
                    text: String,
                    condition: Option[PyExpr],
                    args: Option[ArgumentInfo],
                    startLine: Int,
                    endLine: Int
                   ) extends ASTNode with ChildrenList[ASTNode]

object MenuItem {
  def apply(fileName: String, lastLine: Int, text: String, condition: Option[PyExpr], args: Option[ArgumentInfo], children: List[ASTNode]): MenuItem = {
    val lineList = children.map(_.lineNum)
    val maybeStartLine = lineList.minOption
    val maybeEndLine = lineList.maxOption
    val maybeLineNum = condition match {
      case Some(DebugPyExpr(_, `fileName`, line, _)) if line > lastLine => Some(line)
      case _ => None
    }

    val lineNum = maybeLineNum.orElse(maybeStartLine.map(_ - 1)).getOrElse(lastLine + 1)
    val startLine = maybeStartLine.getOrElse(lineNum)
    val endLine = maybeEndLine.getOrElse(lineNum)

    new MenuItem(children, fileName, lineNum, text, condition, args, startLine, endLine)
  }
}
