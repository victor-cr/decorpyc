package com.codegans.decorpyc.ast

import scala.collection.mutable.ListBuffer

case class Menu(
                 override val attributes: Map[String, _],
                 override val children: List[MenuItem],
                 override val fileName: String,
                 override val lineNum: Int,
                 startStatement: NodeRef,
                 caption: Option[String],
                 args: Option[ArgumentInfo],
                 withA: Option[PyExpr],
                 set: Option[PyExpr],
                 itemArgs: List[Option[ArgumentInfo]]) extends ASTNode with Attributes with ChildrenList[MenuItem]

object Menu extends ASTNodeFactory[Menu] {
  private val keyItems: String = "items"
  private val keyWithA: String = "with_"
  private val keySet: String = "set"
  private val keyArguments: String = "arguments"
  private val keyItemArguments: String = "item_arguments"
  private val keyStatementStart: String = "statement_start"
  private val keyHasCaption: String = "has_caption"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Menu = {
    val withA = context.transformPyExpr(attributes(keyWithA))
    val set = context.transformPyExpr(attributes.get(keySet))
    val args = context.transformArgumentInfo(attributes.get(keyArguments))
    val itemArgs = attributes(keyItemArguments).asInstanceOf[List[_]].map(context.transformArgumentInfo)
    val startStatement = context.ref(attributes(keyStatementStart))
    val hasCaption = attributes(keyHasCaption).asInstanceOf[Boolean]

    val children: ListBuffer[MenuItem] = new ListBuffer()
    var caption: Option[String] = None
    var lastLine = set.map {
      case DebugPyExpr(_, _, exprLine, _) => Math.max(lineNum, exprLine)
      case _ => lineNum
    }.getOrElse(lineNum)

    attributes(keyItems).asInstanceOf[List[_]].zipWithIndex.foreach {
      case ((text: String) :: "True" :: None :: Nil, 0) if hasCaption =>
        lastLine += 1
        caption = Some(text)
      case ((text: String) :: condition :: (block: List[_]) :: Nil, i) =>
        val item = MenuItem(
          fileName,
          lastLine,
          text,
          context.transformPyExpr(condition).filterNot(_.expression == "True"),
          itemArgs.lift(i).flatten,
          block.flatMap(context.transformAST)
        )
        lastLine = item.endLine
        children.addOne(item)
    }

    new Menu(attributes - keyItems - keyWithA - keySet - keyArguments - keyItemArguments - keyStatementStart - keyHasCaption, List.from(children), fileName, lineNum, startStatement, caption, args, withA, set, itemArgs)
  }
}
