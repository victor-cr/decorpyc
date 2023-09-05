package com.codegans.decorpyc

package object format {
  trait Markup extends Ordered[Markup] {
    def indent: Int

    def id: Int

    def value: String

    override def compare(that: Markup): Int = compareTo(that)

    override def compareTo(that: Markup): Int = {
      val primary = Integer.compare(this.indent, that.indent)

      if (primary == 0) Integer.compare(this.id, that.id) else primary
    }
  }

  object Markup {
    def unapply(markup: Markup): Option[(Int, String)] = Some(markup.indent -> markup.value)
  }

  case class Keyword(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Text(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Expr(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Key(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Value(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Comment(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Open(override val indent: Int, override val id: Int, value: String) extends Markup

  case class Next(override val indent: Int, override val id: Int) extends Markup {
    override def value: String = ","
  }

  case class Close(override val indent: Int, override val id: Int, value: String) extends Markup

}
