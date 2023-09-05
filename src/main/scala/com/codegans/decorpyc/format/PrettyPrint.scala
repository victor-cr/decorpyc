package com.codegans.decorpyc.format

import com.codegans.decorpyc.format.PrettyPrint.{indentText, log}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, File, PrintWriter, StringWriter}
import java.nio.charset.Charset

class PrettyPrint(layout: Layout) {
  private def printToWriter(out: PrintWriter): Unit = {
    layout.traverse {
      case (None, (line, Markup(indent, value))) =>
        Iterator.range(1, line).foreach(_ => out.println())
        Iterator.range(0, indent).foreach(_ => out.print(indentText))
        out.print(value)

      case (Some((prevLine, Markup(prevIndent, _))), (currentLine, Markup(currentIndent, value))) if prevLine != currentLine =>
        if (prevIndent < currentIndent) out.print(':')
        Iterator.range(prevLine, currentLine).foreach(_ => out.println())
        Iterator.range(0, currentIndent).foreach(_ => out.print(indentText))
        out.print(value.stripTrailing())

      case (Some((_, _: Expr)), (_, Open(_, _, value))) =>
        out.print(value)
      case (Some((_, _: Markup)), (_, Close(_, _, value))) =>
        out.print(value)
      case (Some((_, _: Open)), (_, _: Next)) =>
        log.debug("Ignore comma before param list")
      case (Some((_, _: Markup)), (_, _: Next)) =>
        out.print(',')
      case (Some((_, _: Open)), (_, Key(_, _, value))) =>
        out.print(value.stripTrailing())
      case (Some((_, _: Open)), (_, Value(_, _, value))) =>
        out.print(value.stripTrailing())
      case (Some((_, _: Open)), (_, Expr(_, _, value))) =>
        out.print(value.stripTrailing())
      case (Some((_, _: Key)), (_, Value(_, _, value))) =>
        out.print('=')
        out.print(value.stripTrailing())
      case (_, (_, Markup(_, value))) =>
        out.print(' ')
        out.print(value.stripTrailing())
    }
  }

  def printToString: String = {
    val out = new StringWriter()
    val temp = new PrintWriter(out)
    try {
      printToWriter(temp)
      temp.flush()
    } finally {
      temp.close()
    }
    out.toString
  }

  def printToByteArray(charset: Charset): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val temp = new PrintWriter(out, false, charset)
    try {
      printToWriter(temp)
      temp.flush()
    } finally {
      temp.close()
    }
    out.toByteArray
  }

  def printToFile(file: File, charset: Charset): Unit = {
    val temp = new PrintWriter(file, charset)
    try {
      printToWriter(temp)
      temp.flush()
    } finally {
      temp.close()
    }
  }
}

object PrettyPrint {
  private val log: Logger = LoggerFactory.getLogger(classOf[Printer])

  private val indentChar = ' '
  private val indentSize = 4
  private val indentText = Iterator.fill(indentSize)(indentChar).mkString
}
