package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.atl.ATLNode

case class Show(override val attributes: Map[String, _],
                override val atl: List[ATLNode],
                override val fileName: String,
                override val lineNum: Int,
                imSpec: List[_]
               ) extends ASTNode with Attributes with Transformations

object Show extends ASTNodeFactory[Show] {
  private val keyATL: String = "atl"
  private val keyIMSpec: String = "imspec"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Show = {
    val atl = context.transformATL(attributes.get(keyATL))
    val imspec = attributes(keyIMSpec).asInstanceOf[List[_]]

    new Show(attributes - keyATL - keyIMSpec, atl, fileName, lineNum, imspec)
  }
}
