package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, NodeContext, PyCode}

case class SLPython(override val attributes: Map[String, _],
                    override val fileName: String,
                    override val lineNum: Int,
                    code: Option[PyCode]
                   ) extends SLNode with Attributes

object SLPython extends SLNodeFactory[SLPython] {
  private val keyCode: String = "code"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLPython = {
    val code = context.transformPyCode(attributes(keyCode))

    new SLPython(attributes - keyCode, fileName, lineNum, code)
  }
}
