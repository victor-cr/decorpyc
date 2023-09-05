package com.codegans.decorpyc.ast

case class Init private(override val attributes: Map[String, _],
                        override val children: List[ASTNode],
                        override val fileName: String,
                        override val lineNum: Int,
                        priority: Int
                       ) extends ASTNode with Attributes with ChildrenList[ASTNode]

object Init extends ASTNodeFactory[Init] {
  private val keyBlock: String = "block"
  private val keyPriority: String = "priority"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Init = {
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)
    val priority = attributes(keyPriority).asInstanceOf[Int]

    new Init(attributes - keyBlock - keyPriority, children, fileName, lineNum, priority)
  }
}
