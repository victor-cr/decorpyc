package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.atl.ATLNode

case class Image(
                  override val attributes: Map[String, _],
                  override val atl: List[ATLNode],
                  override val fileName: String,
                  override val lineNum: Int,
                  imageName: List[String],
                  code: Option[PyCode]
                ) extends ASTNode with Attributes with Transformations

object Image extends ASTNodeFactory[Image] {
  private val keyImgName: String = "imgname"
  private val keyATL: String = "atl"
  private val keyCode: String = "code"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Image = {
    val imageName = attributes("imgname").asInstanceOf[List[String]]
    val atl = attributes.get("atl").map(context.transformATL).getOrElse(Nil)
    val code = context.transformPyCode(attributes.get("code"))

    new Image(attributes - keyImgName - keyATL - keyCode, atl, fileName, lineNum, imageName, code)
  }
}
