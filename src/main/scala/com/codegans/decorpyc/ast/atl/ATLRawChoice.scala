package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, ChildrenList, Node, NodeContext}

case class ATLRawChoice(override val attributes: Map[String, _],
                        override val children: List[ATLRawChoice.Item],
                        override val fileName: String,
                        override val lineNum: Int) extends ATLNode with Attributes with ChildrenList[ATLRawChoice.Item]

object ATLRawChoice extends ATLNodeFactory[ATLRawChoice] {
  private val keyChoices: String = "choices"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawChoice = {
    val children = attributes(keyChoices).asInstanceOf[List[_]].map {
      case (chance: Float) :: atl :: Nil => Item(chance.toString, context.transformATL(atl).head)
      case (chance: Double) :: atl :: Nil => Item(chance.toString, context.transformATL(atl).head)
      case (chance: String) :: atl :: Nil => Item(chance, context.transformATL(atl).head)
    }

    new ATLRawChoice(attributes - keyChoices, children, fileName, lineNum)
  }

  case class Item(chance: String, atl: ATLNode) extends Node
}
