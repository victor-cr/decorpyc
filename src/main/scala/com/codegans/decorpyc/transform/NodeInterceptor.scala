package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast.atl.ATLNode
import com.codegans.decorpyc.ast.sl.SLNode
import com.codegans.decorpyc.ast.{ASTNode, Body, Header}

trait NodeInterceptor {
  def replace(node: Header): Header = node

  def replace(node: Body): Body = node

  def replace(node: ASTNode): ASTNode = node

  def replace(node: ATLNode): ATLNode = node

  def replace(node: SLNode): SLNode = node
}

object NodeInterceptor {
  def apply(aspects: Aspect[_]*): NodeInterceptor = new NodeInterceptor {
    override def replace(node: Header): Header = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[Header]].replace(result)
      case (result, _) => result
    }

    override def replace(node: Body): Body = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[Body]].replace(result)
      case (result, _) => result
    }

    override def replace(node: ASTNode): ASTNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[ASTNode]].replace(result)
      case (result, _) => result
    }

    override def replace(node: ATLNode): ATLNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[ATLNode]].replace(result)
      case (result, _) => result
    }

    override def replace(node: SLNode): SLNode = aspects.foldLeft(node) {
      case (result, aspect) if aspect.isApplicable(result) => aspect.asInstanceOf[Aspect[SLNode]].replace(result)
      case (result, _) => result
    }
  }
}