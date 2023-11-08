package com.codegans.decorpyc.ast

case class TranslateBlock(
                           override val attributes: Map[String, _],
                           override val children: List[ASTNode],
                           override val fileName: String,
                           override val lineNum: Int,
                           language: String) extends ASTNode with Attributes with ChildrenList[ASTNode]

object TranslateBlock extends ASTNodeFactory[TranslateBlock] {
  val keyBlock = "block"
  val keyLanguage = "language"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): TranslateBlock = {
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)
    val language = context.transformString(attributes(keyLanguage)).getOrElse("english")

    new TranslateBlock(attributes - keyBlock - keyLanguage, children, fileName, lineNum, language)
  }
}
