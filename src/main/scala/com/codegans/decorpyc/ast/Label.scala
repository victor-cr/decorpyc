package com.codegans.decorpyc.ast

case class Label private(override val attributes: Map[String, _],
                         override val children: List[ASTNode],
                         override val fileName: String,
                         override val lineNum: Int,
                         labelName: String,
                         params: Option[ParameterInfo]
                        ) extends ASTNode with Attributes with ChildrenList[ASTNode]

object Label extends ASTNodeFactory[Label] {
  private val keyBlock: String = "block"
  private val keyParameters: String = "parameters"
  private val keyName: String = "name"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Label = {
    val children = attributes(keyBlock).asInstanceOf[List[_]].flatMap(context.transformAST)
    val params = context.transformParameterInfo(attributes.get(keyParameters))
    val name = attributes(keyName).asInstanceOf[String]

    new Label(attributes - keyBlock - keyParameters - keyName, children, fileName, lineNum, name, params)
  }
}