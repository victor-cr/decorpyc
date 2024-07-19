package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{ASTNode, ArgumentInfo, Attributes, NodeContext, PyExpr}

case class SLUse(override val attributes: Map[String, _],
                 override val fileName: String,
                 override val lineNum: Int,
                 ast: List[ASTNode],
                 block: List[SLNode],
                 id: Option[PyExpr],
                 args: Option[ArgumentInfo],
                 target: Option[PyExpr]
                ) extends SLNode with Attributes

object SLUse extends SLNodeFactory[SLUse] {
  private val keyAST: String = "ast"
  private val keyBlock: String = "block"
  private val keyId: String = "id"
  private val keyArgs: String = "args"
  private val keyTarget: String = "target"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLUse = {
    val ast = context.transformAST(attributes(keyAST))
    val block = context.transformSL(attributes(keyBlock))
    val id = attributes.get(keyId).flatMap(context.transformPyExpr)
    val args = context.transformArgumentInfo(attributes.get(keyArgs))
    val target = attributes.get(keyTarget).flatMap(context.transformPyExpr)

    new SLUse(attributes - keyAST - keyBlock - keyId - keyArgs - keyTarget, fileName, lineNum, ast, block, id, args, target)
  }
}
