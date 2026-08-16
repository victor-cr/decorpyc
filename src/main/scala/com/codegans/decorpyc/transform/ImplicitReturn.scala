package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast.Body

object ImplicitReturn extends Aspect[Body] {
  override def isApplicable(node: ast.Node): Boolean = node.isInstanceOf[Body]

  override def replace(node: Body, version: Int): Body = {
    node.children.last match {
      case _: ast.Return => node.copy(children = node.children.init)
      case _ => node
    }
  }
}
