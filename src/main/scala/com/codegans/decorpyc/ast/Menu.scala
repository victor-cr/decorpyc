package com.codegans.decorpyc.ast

import scala.collection.mutable.ListBuffer

case class Menu(
                 override val attributes: Map[String, _],
                 override val children: List[MenuItem],
                 override val fileName: String,
                 override val lineNum: Int,
                 startStatement: Any,
                 hasCaption: Boolean,
                 args: Option[_],
                 withA: Option[PyExpr],
                 set: Option[_],
                 itemArgs: List[Option[ArgumentInfo]]
               ) extends ASTNode with Attributes with ChildrenList[MenuItem]

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
    val set = attributes(keySet).asInstanceOf[Option[_]]
    val args = context.transformArgumentInfo(attributes.get(keyArguments))
    val itemArgs = attributes(keyItemArguments).asInstanceOf[List[_]].map(context.transformArgumentInfo)
    val startStatement = attributes(keyStatementStart)
    val hasCaption = attributes(keyHasCaption).asInstanceOf[Boolean]

    val children: ListBuffer[MenuItem] = new ListBuffer()
    var lastLine = lineNum

    attributes(keyItems).asInstanceOf[List[_]].foreach {
      case (text: String) :: condition :: (block: List[_]) :: Nil =>
        val item = MenuItem(
          fileName,
          lastLine,
          text,
          context.transformPyExpr(condition),
          block.flatMap(context.transformAST)
        )
        lastLine = item.endLine
        children.addOne(item)
    }

    new Menu(attributes - keyItems - keyWithA - keySet - keyArguments - keyItemArguments - keyStatementStart - keyHasCaption, List.from(children), fileName, lineNum, startStatement, hasCaption, args, withA, set, itemArgs)
  }
}
