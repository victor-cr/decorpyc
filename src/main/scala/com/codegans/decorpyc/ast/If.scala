package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.IfCondition.ConditionType

import scala.collection.mutable.ListBuffer

case class If(override val attributes: Map[String, _],
              override val children: List[IfCondition],
              override val fileName: String,
              override val lineNum: Int
             ) extends ASTNode with Attributes with ChildrenList[IfCondition]

object If extends ASTNodeFactory[If] {
  private val keyEntries: String = "entries"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): If = {
    val children: ListBuffer[IfCondition] = ListBuffer()
    var lastLine = lineNum
    val entries = attributes(keyEntries).asInstanceOf[List[_]]

    entries.zipWithIndex.foreach {
      case (condition :: (block: List[_]) :: Nil, 0) =>
        val expr = context.transformPyExpr(condition)
        val ifBlock = block.flatMap(context.transformAST)

        val child = IfCondition(fileName, lastLine, ConditionType.IF, expr, ifBlock)

        lastLine = child.endLine
        children.addOne(child)
      case (("True" | None) :: (block: List[_]) :: Nil, i) if i + 1 == entries.size =>
        val elseBlock = block.flatMap(context.transformAST)

        val child = IfCondition(fileName, lastLine, ConditionType.ELSE, None, elseBlock)

        lastLine = child.endLine
        children.addOne(child)
      case (condition :: (block: List[_]) :: Nil, _) =>
        val expr = context.transformPyExpr(condition)
        val elifBlock = block.flatMap(context.transformAST)

        val child = IfCondition(fileName, lastLine, ConditionType.ELIF, expr, elifBlock)

        lastLine = child.endLine
        children.addOne(child)
    }

    new If(attributes - keyEntries, List.from(children), fileName, lineNum)
  }
}
