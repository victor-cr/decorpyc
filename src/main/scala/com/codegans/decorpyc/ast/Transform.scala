package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.atl.ATLNode

case class Transform(
                      override val attributes: Map[String, _],
                      override val atl: List[ATLNode],
                      override val fileName: String,
                      override val lineNum: Int,
                      variable: String,
                      params: Option[ParameterInfo]
                    ) extends ASTNode with Attributes with Transformations

object Transform extends ASTNodeFactory[Transform] {
  private val keyATL: String = "atl"
  private val keyVarName: String = "varname"
  private val keyParameters: String = "parameters"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Transform = {
    val variable = attributes(keyVarName).asInstanceOf[String]
    val atl = attributes.get(keyATL).map(context.transformATL).getOrElse(Nil)
    val params = context.transformParameterInfo(attributes.get(keyParameters))

    new Transform(attributes - keyATL - keyVarName - keyParameters, atl, fileName, lineNum, variable, params)
  }
}
