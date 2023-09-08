package com.codegans.decorpyc.format

import com.codegans.decorpyc.ast.{ArgumentInfo, ParameterInfo, PyExpr}
import com.codegans.decorpyc.format.Layout.{Callback, Record, log, possibleExclusiveCommandConflict}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class Layout(doubleQuotedText: Boolean = true) {
  private var id = 0
  private val lines: mutable.SortedMap[Int, mutable.SortedSet[Markup]] = new mutable.TreeMap[Int, mutable.SortedSet[Markup]]()

  private def rawPrint(line: Int, value: Markup): Layout = {
    lines.getOrElseUpdate(line, new mutable.TreeSet[Markup]()).addOne(value)
    id += 1
    this
  }

  def printKeyword(line: Int, indent: Int, keyword: String, exclusive: Boolean = false): Layout = {
    if (exclusive && lines.get(line).exists(_.nonEmpty)) {
      if (possibleExclusiveCommandConflict.contains(keyword)) {
        log.debug("Ignoring attempt to write exclusive command `{}` at already populated line #{}", keyword, line)
      } else {
        log.warn("Ignoring attempt to write exclusive command `{}` at already populated line #{}", keyword, line)
      }
      this
    } else {
      rawPrint(line, Keyword(indent, id, keyword))
    }
  }

  def printArgs(line: Int, indent: Int, element: ParameterInfo): Layout = {
    printOpen(line, indent)
    element.params.zipWithIndex.foreach { case ((key, maybeValue), i) =>
      if (i != 0) printNext(line, indent)
      printKey(line, indent, key)
      maybeValue.foreach(printValue(line, indent, _))
    }
    printClose(line, indent)
  }

  def printArgs(line: Int, indent: Int, value: ArgumentInfo): Layout = {
    printOpen(line, indent)
    value.arguments.zipWithIndex.foreach { case ((maybeKey, maybeValue), i) =>
      if (i != 0) printNext(line, indent)
      maybeKey.foreach(printKey(line, indent, _))
      maybeValue.foreach(printValue(line, indent, _))
    }
    printClose(line, indent)
  }

  def printText(line: Int, indent: Int, text: String): Layout = {
    val buf = new StringBuilder()

    if (doubleQuotedText) buf.append('"') else buf.append('\'')

    text.foreach {
      case '\\' => buf.append("\\\\")
      case '\n' => buf.append("\\n")
      case '"' if doubleQuotedText => buf.append("\\\"")
      case '\'' if !doubleQuotedText => buf.append("\\\'")
      case ch => buf.append(ch)
    }

    if (doubleQuotedText) buf.append('"') else buf.append('\'')

    rawPrint(line, Text(indent, id, buf.toString()))
  }

  def printMultilineText(line: Int, indent: Int, text: String): Layout = {
    val buf = new StringBuilder()

    if (doubleQuotedText) {
      rawPrint(line, Text(indent, id, "\"\"\""))
    } else {
      rawPrint(line, Text(indent, id, "'''"))
    }

    text.foreach {
      case '\\' => buf.append("\\\\")
      case '"' if doubleQuotedText => buf.append("\\\"")
      case '\'' if !doubleQuotedText => buf.append("\\\'")
      case ch => buf.append(ch)
    }

    val list = buf.toString().linesIterator.toList

    list.zipWithIndex
      .filterNot { case (text, _) => text.isBlank }
      .foreach { case (text, i) => rawPrint(line + i + 1, Text(indent, id, text)) }

    if (doubleQuotedText) {
      rawPrint(line + list.size + 1, Text(indent, id, "\"\"\""))
    } else {
      rawPrint(line + list.size + 1, Text(indent, id, "'''"))
    }
  }

  def printExpr(line: Int, indent: Int, expr: String): Layout = rawPrint(line, Expr(indent, id, expr))

  def printExpr(line: Int, indent: Int, expr: Int): Layout = rawPrint(line, Expr(indent, id, String.valueOf(expr)))

  def printExpr(line: Int, indent: Int, expr: PyExpr): Layout = rawPrint(line, Expr(indent, id, expr.expression)) //TODO: maybe validate positiona

  def printOpen(line: Int, indent: Int, value: String = "("): Layout = rawPrint(line, Open(indent, id, value))

  def printNext(line: Int, indent: Int): Layout = rawPrint(line, Next(indent, id))

  def printKey(line: Int, indent: Int, expr: String): Layout = rawPrint(line, Key(indent, id, expr))

  def printValue(line: Int, indent: Int, expr: String): Layout = rawPrint(line, Value(indent, id, expr))

  def printValue(line: Int, indent: Int, expr: PyExpr): Layout = rawPrint(line, Value(indent, id, expr.expression)) //TODO: maybe validate position

  def printClose(line: Int, indent: Int, value: String = ")"): Layout = rawPrint(line, Close(indent, id, value))

  def printComment(line: Int, indent: Int, comment: String): Layout = rawPrint(line, Comment(Int.MaxValue, id, comment))

  def traverse(callback: Callback): Layout = {
    var prev: Option[Record] = None

    lines.foreach { case (line, commands) =>
      commands.foreach { command =>
        val record: Record = line -> command
        callback(prev, record)
        prev = Some(record)
      }
    }

    this
  }

}

object Layout {
  private val log: Logger = LoggerFactory.getLogger(classOf[Layout])
  private val possibleExclusiveCommandConflict: Seq[String] = Seq("pass", "return")

  type Record = (Int, Markup)
  type Callback = (Option[Record], Record) => Unit
}
