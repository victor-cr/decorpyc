package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast.{ASTNode, Body, ChildrenList, IfCondition, Init, Label, MenuItem, Node, Say, Translate, UserStatement}

import scala.annotation.switch
import scala.collection.mutable.ListBuffer

object MultilineSayAspect {
  private val separator: String = System.lineSeparator() + System.lineSeparator()

  val Body: Aspect[Body] = new Aspect[Body] {
    override def isApplicable(node: Node): Boolean = node.isInstanceOf[Body]

    override def replace(node: Body): Body = internalReplace(node, node.children, e => node.copy(children = e))
  }

  val AST: Aspect[ASTNode] = new Aspect[ASTNode] {
    override def isApplicable(node: Node): Boolean = (node: @switch) match {
      case parent: Init => true
      case parent: Label => true
      case parent: IfCondition => true
      case parent: MenuItem => true
      case parent: Translate => true
      case parent: UserStatement => true
      case _ => false
    }

    override def replace(node: ast.ASTNode): ast.ASTNode = node match {
      case parent: Init => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case parent: Label => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case parent: IfCondition => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case parent: MenuItem => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case parent: Translate => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case parent: UserStatement => internalReplace(parent, parent.children, e => parent.copy(children = e))
      case _ => node
    }
  }

  private def internalReplace[T <: Node](parent: T, children: List[ASTNode], fn: List[ASTNode] => T): T = {
    val sayMap = children.filter(_.isInstanceOf[Say]).groupBy(_.lineNum).filter { case (_, list) => list.size > 1 }

    if (sayMap.nonEmpty) {
      val result: ListBuffer[ASTNode] = ListBuffer()
      var lastSay: Option[Say] = None

      children.foreach {
        case say@Say(_, _, lineNum, _, _, _, _, _, _, _, _, false) if sayMap.contains(lineNum) =>
          val empty = say.copy(attributes = Map(), what = "")

          sayMap.get(lineNum).filterNot(_ => lastSay.contains(empty)).foreach { case list: List[Say] =>
            lastSay = Some(empty)
            result.addOne(empty.copy(multiline = true, what = list.map(_.what).mkString(separator)))
          }
        case child =>
          lastSay = None
          result.addOne(child)
      }
      fn(List.from(result))
    } else {
      parent
    }
  }
}
