package com.codegans.decorpyc.ast

case class UserStatement(override val attributes: Map[String, _],
                         override val children: List[ASTNode],
                         override val fileName: String,
                         override val lineNum: Int,
                         line: String,
                         translatable: Boolean,
                         parsed: List[_],
                         subParses: List[_]
                        ) extends ASTNode with Attributes with ChildrenList[ASTNode]

object UserStatement extends ASTNodeFactory[UserStatement] {
  private val keyBlock: String = "block"
  private val keyParsed: String = "parsed"
  private val keySubParses: String = "subparses"
  private val keyTranslatable: String = "translatable"
  private val keyLine: String = "line"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): UserStatement = {
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)
    val parsed = context.transformList(attributes(keyParsed))
    val subParses = context.transformList(attributes.get(keySubParses))
    val translatable = attributes(keyTranslatable).asInstanceOf[Boolean]
    val line = attributes(keyLine).asInstanceOf[String]

    new UserStatement(attributes - keyBlock - keyParsed - keySubParses - keyTranslatable - keyLine, children, fileName, lineNum, line, translatable, parsed, subParses)
  }
}
