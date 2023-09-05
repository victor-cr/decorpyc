package com.codegans.decorpyc.ast.sl

import com.codegans.decorpyc.ast.{Attributes, NodeContext}

case class SLTransclude(override val attributes: Map[String, _],
                        override val fileName: String,
                        override val lineNum: Int
                       ) extends SLNode with Attributes

object SLTransclude extends SLNodeFactory[SLTransclude] {
  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): SLTransclude =
    new SLTransclude(attributes, fileName, lineNum)
}
