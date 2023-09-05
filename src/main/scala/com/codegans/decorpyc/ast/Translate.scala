package com.codegans.decorpyc.ast

case class Translate(
                      override val attributes: Map[String, _],
                      override val children: List[ASTNode],
                      override val fileName: String,
                      override val lineNum: Int,
                      identifier: String,
                      alternate: Option[String],
                      language: Option[String]
                    ) extends ASTNode with Attributes with ChildrenList[ASTNode]

object Translate extends ASTNodeFactory[Translate] {
  private val keyBlock: String = "block"
  private val keyIdentifier: String = "identifier"
  private val keyAlternate: String = "alternate"
  private val keyLanguage: String = "language"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Translate = {
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)
    val identifier = attributes(keyIdentifier).asInstanceOf[String]
    val alternate = context.transformString(attributes.get(keyAlternate))
    val language = context.transformString(attributes.get(keyLanguage))

    new Translate(attributes - keyBlock - keyIdentifier - keyAlternate - keyLanguage, children, fileName, lineNum, identifier, alternate, language)
  }
}
