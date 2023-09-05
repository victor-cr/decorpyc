package com.codegans.decorpyc.ast

case class Style(
                  override val attributes: Map[String, _],
                  override val fileName: String,
                  override val lineNum: Int,
                  styleName: String,
                  parent: Option[String],
                  clear: Boolean,
                  delAttrs: List[_],
                  variant: Option[PyExpr],
                  props: Map[String, Option[PyExpr]],
                  take: Option[_]
                ) extends ASTNode with Attributes

object Style extends ASTNodeFactory[Style] {
  private val keyParent: String = "parent"
  private val keyClear: String = "clear"
  private val keyDelAttr: String = "delattr"
  private val keyVariant: String = "variant"
  private val keyTake: String = "take"
  private val keyStyleName: String = "style_name"
  private val keyProperties: String = "properties"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): Style = {
    val parent = context.transformString(attributes.get(keyParent))
    val clear = attributes(keyClear).asInstanceOf[Boolean]
    val delAttrs = context.transformStringList(attributes.get(keyDelAttr))
    val variant = context.transformPyExpr(attributes(keyVariant))
    val take = attributes(keyTake).asInstanceOf[Option[_]]
    val styleName = attributes(keyStyleName).asInstanceOf[String]
    val properties = context.transformStringMap(attributes.get(keyProperties)).map {
      case (key, value) => key -> context.transformPyExpr(value)
    }

    new Style(attributes - keyParent - keyClear - keyDelAttr - keyVariant - keyTake - keyStyleName - keyProperties, fileName, lineNum, styleName, parent, clear, delAttrs, variant, properties, take)
  }
}
