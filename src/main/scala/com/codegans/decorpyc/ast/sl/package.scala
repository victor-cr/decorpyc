package com.codegans.decorpyc.ast

package object sl {
  trait SLNode extends Node with DebugInfo

  trait SLNodeFactory[T <: SLNode] extends DebugFactoryFunction[T] {

    override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): T
  }
}
