package com.codegans.decorpyc.ast.atl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyExpr}

case class ATLRawMultipurpose(
                               override val attributes: Map[String, _],
                               override val fileName: String,
                               override val lineNum: Int,
                               duration: Option[PyExpr],
                               splines: Map[String, List[PyExpr]],
                               revolution: Option[String],
                               warpFunction: Option[_],
                               warper: Option[String],
                               expressions: List[(Option[PyExpr], Option[PyExpr])],
                               circles: Option[PyExpr],
                               props: Map[String, Option[PyExpr]]
                             ) extends ATLNode with Attributes

object ATLRawMultipurpose extends ATLNodeFactory[ATLRawMultipurpose] {
  private val keyDuration: String = "duration"
  private val keyCircles: String = "circles"
  private val keySplines: String = "splines"
  private val keyExpressions: String = "expressions"
  private val keyProperties: String = "properties"
  private val keyRevolution: String = "revolution"
  private val keyWarpFunction: String = "warp_function"
  private val keyWarper: String = "warper"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): ATLRawMultipurpose = {
    val duration = context.transformPyExpr(attributes(keyDuration))
    val expressions = attributes(keyExpressions).asInstanceOf[List[_]]
    val properties = attributes(keyProperties).asInstanceOf[List[_]]
    val revolution = context.transformString(attributes(keyRevolution))
    val warpFunction = attributes(keyWarpFunction).asInstanceOf[Option[_]]
    val warper = context.transformString(attributes.get(keyWarper))

    val props = properties.map { case (key: String) :: expr :: Nil => key -> context.transformPyExpr(expr) }.toMap
    val exprs = expressions.map { case exprL :: exprR :: Nil => (context.transformPyExpr(exprL), context.transformPyExpr(exprR)) }

    val circles = context.transformPyExpr(attributes(keyCircles)).flatMap {
      case PyExpr("0") => None
      case value => Some(value)
    }
    val splines = attributes(keySplines).asInstanceOf[List[_]].map {
      case key :: (value: List[_]) :: Nil =>
        val list = value.flatMap(context.transformPyExpr)

        key.asInstanceOf[String] -> list.zipWithIndex.sortBy { case (_, i) => (i + 1) % list.size }.map { case (expr, _) => expr }
    }.toMap

    new ATLRawMultipurpose(attributes - keyDuration - keyCircles - keySplines - keyExpressions - keyProperties - keyRevolution - keyWarpFunction - keyWarper, fileName, lineNum, duration, splines, revolution, warpFunction, warper, exprs, circles, props)
  }
}
