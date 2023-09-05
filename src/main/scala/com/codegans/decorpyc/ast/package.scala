package com.codegans.decorpyc

import com.codegans.decorpyc.ast.atl.ATLNode
import com.codegans.decorpyc.ast.sl.SLNode

package object ast {

  type FactoryFunction[T <: Node] = (NodeContext, Map[String, _]) => T
  type DebugFactoryFunction[T <: Node] = (NodeContext, Map[String, _], String, Int) => T

  trait DebugInfo {
    def fileName: String

    def lineNum: Int
  }

  object DebugInfo {
    def unapply(info: ASTNode): Option[(String, Int)] = Some(info.fileName -> info.lineNum)

    def unapply(info: ATLNode): Option[(String, Int)] = Some(info.fileName -> info.lineNum)

    def unapply(info: SLNode): Option[(String, Int)] = Some(info.fileName -> info.lineNum)
  }

  trait Node

  class NodeRef(resolve: => Node) {
    lazy val ref: Node = resolve
  }

  object NodeRef {
    def unapply(node: NodeRef): Option[Node] = Some(node.ref)
  }

  trait ASTNode extends Node with DebugInfo

  trait Attributes {
    self: Node =>
    def attributes: Map[String, _]
  }

  trait Transformations {
    self: Node =>
    def atl: List[ATLNode]
  }

  trait ChildrenList[T <: Node] {
    self: Node =>
    def children: List[T]
  }

  trait ChildrenMap[T <: Node] {
    self: Node =>
    def children: Map[String, T]
  }

  case class Root(header: Header, body: Body) extends Node

  case class Body(override val children: List[ASTNode]) extends Node with ChildrenList[ASTNode]

  case class Header(override val attributes: Map[String, _], version: Int, key: String) extends Node with Attributes

  object Header {
    def apply(attributes: Map[String, _], version: Int, key: String): Header =
      new Header(attributes - "key" - "version", version, key)
  }

  trait NodeContext {
    def ref(id: Int): NodeRef

    def ref(instance: Any): NodeRef

    def transformAST: PartialFunction[Any, List[ASTNode]]

    def transformATL: PartialFunction[Any, List[ATLNode]]

    def transformSL: PartialFunction[Any, List[SLNode]]

    def transformPyExpr: PartialFunction[Any, Option[PyExpr]]

    def transformPyCode: PartialFunction[Any, Option[PyCode]]

    def transformArgumentInfo: PartialFunction[Any, Option[ArgumentInfo]]

    def transformParameterInfo: PartialFunction[Any, Option[ParameterInfo]]

    def transformString: PartialFunction[Any, Option[String]] = {
      case value: String => Some(value)
      case None => None
      case Some(value) => transformString(value)
    }

    def transformStringList: PartialFunction[Any, List[String]] = {
      case value: String => List(value)
      case value: List[String] => value
      case None => Nil
      case Some(value) => transformStringList(value)
    }

    def transformStringMap: PartialFunction[Any, Map[String, _]] = {
      case value: Map[String, _] => value
      case None => Map.empty
      case Some(value) => transformStringMap(value)
    }
  }

  trait ASTNodeFactory[T <: ASTNode] extends DebugFactoryFunction[T] {
    override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): T
  }

  trait PyCode extends Node {
    def source: PyExpr

    def mode: String

    def py: Int
  }

  case class OnelinerPyCode(override val source: PyExpr, override val mode: String, override val py: Int) extends PyCode

  case class BlockPyCode(override val source: PyExpr, override val mode: String, override val py: Int, lines: Map[Int, String]) extends PyCode

  object PyCode {
    def apply(source: PyExpr, mode: String, py: Int): PyCode = source match {
      case expr@PyExpr(expression) =>
        val lines = expression.linesIterator.zipWithIndex
          .filterNot { case (text, _) => text.isBlank }
          .map { case (text, i) => text -> i }
          .toList

        if (lines.size == 1 && lines.head._2 == 0) {
          OnelinerPyCode(expr, mode, py)
        } else if (lines.nonEmpty) {
          BlockPyCode(expr, mode, py, lines.map { case (text, line) => line -> text }.toMap)
        } else {
          throw new IllegalArgumentException("PyCode block is blank")
        }
    }

    def unapply(code: PyCode): Option[(PyExpr, String)] = Some(code.source -> code.mode)
  }

  trait PyExpr extends Node {
    def expression: String
  }

  object PyExpr {
    def unapply(pyExpr: PyExpr): Option[String] = Some(pyExpr.expression)
  }

  case class StringPyExpr(override val expression: String) extends PyExpr

  case class DebugPyExpr(override val expression: String, override val fileName: String, override val lineNum: Int, py: Int) extends PyExpr with DebugInfo

  case class ParameterInfo(attributes: Map[String, _], params: Map[String, Option[PyExpr]]) extends Node

  case class ArgumentInfo(attributes: Map[String, _],
                          arguments: List[(Option[String], Option[PyExpr])],
                          starredIndexes: Any,
                          doubleStarredIndexes: Any,
                          version: Int
                         ) extends Node

}
