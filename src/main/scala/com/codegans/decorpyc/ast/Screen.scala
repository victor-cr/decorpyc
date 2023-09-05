package com.codegans.decorpyc.ast

import com.codegans.decorpyc.ast.sl.SLNode

case class Screen(override val attributes: Map[String, _],
                  override val fileName: String,
                  override val lineNum: Int,
                  screen: List[SLNode]
                 ) extends ASTNode with Attributes

object Screen extends ASTNodeFactory[Screen] {
  private val keyScreen: String = "screen"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Screen = {
    val screen = context.transformSL(attributes(keyScreen))

    new Screen(attributes - keyScreen, fileName, lineNum, screen)
  }
}
