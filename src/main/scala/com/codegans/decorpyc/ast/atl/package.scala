package com.codegans.decorpyc.ast

package object atl {
  trait ATLNode extends Node with DebugInfo

  trait ATLNodeFactory[T <: ATLNode] extends DebugFactoryFunction[T] {
    override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): T
  }
}
