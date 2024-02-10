package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast._

case class SLScreen(override val attributes: Map[String, _],
                    override val children: List[SLNode],
                    override val fileName: String,
                    override val lineNum: Int,
                    name: String,
                    layer: String,
                    tag: Option[String],
                    zOrder: Option[PyExpr],
                    params: Option[ParameterInfo]
                   ) extends SLNode with Attributes with ChildrenList[SLNode]

object SLScreen extends SLNodeFactory[SLScreen] {
  private val keyName: String = "name"
  private val keyTag: String = "tag"
  private val keyLayer: String = "layer"
  private val keyZOrder: String = "zorder"
  private val keyParameters: String = "parameters"
  private val keyChildren: String = "children"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLScreen = {
    val name = attributes(keyName).asInstanceOf[String]
    val layer = attributes(keyLayer) match {
      case value: String => value
      case value => context.transformPyExpr(value).map(e => e.expression).getOrElse("")
    }
    val tag = context.transformString(attributes.get(keyTag))
    val zOrder = context.transformPyExpr(attributes.get(keyZOrder))
    val params = context.transformParameterInfo(attributes.get(keyParameters))
    val children = attributes(keyChildren).asInstanceOf[List[_]].flatMap(context.transformSL)

    new SLScreen(attributes - keyName - keyLayer - keyTag - keyZOrder - keyParameters - keyChildren, children, fileName, lineNum, name, layer, tag, zOrder, params)
  }
}
