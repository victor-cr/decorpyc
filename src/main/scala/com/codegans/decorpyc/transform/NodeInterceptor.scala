package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast.atl.ATLNode
import com.codegans.decorpyc.ast.sl.SLNode
import com.codegans.decorpyc.ast.{ASTNode, Body, Header}

trait NodeInterceptor {
  def replace(node: Header, version: Int): Header = node

  def replace(node: Body, version: Int): Body = node

  def replace(node: ASTNode, version: Int): ASTNode = node

  def replace(node: ATLNode, version: Int): ATLNode = node

  def replace(node: SLNode, version: Int): SLNode = node
}

object NodeInterceptor {
  def apply(aspects: Aspect[_]*): NodeInterceptor = new NodeInterceptor {
    override def replace(node: Header, version: Int): Header = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[Header]].replace(result, version)
      case (result, _) => result
    }

    override def replace(node: Body, version: Int): Body = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[Body]].replace(result, version)
      case (result, _) => result
    }

    override def replace(node: ASTNode, version: Int): ASTNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[ASTNode]].replace(result, version)
      case (result, _) => result
    }

    override def replace(node: ATLNode, version: Int): ATLNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[ATLNode]].replace(result, version)
      case (result, _) => result
    }

    override def replace(node: SLNode, version: Int): SLNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[SLNode]].replace(result, version)
      case (result, _) => result
    }
  }
}