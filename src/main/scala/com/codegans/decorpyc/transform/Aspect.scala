package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast.Node

trait Aspect[T <: Node] {
  def isApplicable(node: Node): Boolean

  def replace(node: T): T
}
