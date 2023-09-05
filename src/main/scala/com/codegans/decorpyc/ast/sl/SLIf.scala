package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, Node, NodeContext, PyExpr}

case class SLIf(override val attributes: Map[String, _],
                override val fileName: String,
                override val lineNum: Int,
                entries: Map[Option[PyExpr], List[SLNode]]
               ) extends SLNode with Attributes

object SLIf extends SLNodeFactory[SLIf] {
  private val keyEntries: String = "entries"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLIf = {
    val entries = attributes(keyEntries).asInstanceOf[List[List[_]]].map {
      case condition :: action :: Nil => context.transformPyExpr(condition) -> context.transformSL(action)
    }.toMap

    new SLIf(attributes - keyEntries, fileName, lineNum, entries)
  }
}
