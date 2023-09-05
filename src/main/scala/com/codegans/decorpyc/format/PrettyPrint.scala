package com.codegans.decorpyc.format

import com.codegans.decorpyc.format.PrettyPrint.{indentText, log}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, File, PrintWriter, StringWriter}
import java.nio.charset.Charset

class PrettyPrint(layout: Layout) {

  //  private def pythonEscape(text: String): String = {
  //    val buf = new mutable.StringBuilder
  //    text.foreach {
  //      case '\"' => buf.append("\\\"")
  //      case '\\' => buf.append("\\\\")
  //      case '\n' => buf.append("\\\"")
  //      case '\r' => buf.append("\\\"")
  //      case '\t' => buf.append("\\\"")
  //      case '\b' => buf.append("\\\"")
  //      case '\f' => buf.append("\\\"")
  //      case '\"' => buf.append("\\\"")
  //    }
  //    buf.toString()
  //  }
  private def printToWriter(out: PrintWriter): Unit = {
    var lastLineNum = 1
    var lastIndent = 0

    layout.traverse {
      case (None, (line, Markup(indent, value))) =>
        Iterator.range(1, line).foreach(_ => out.println())
        Iterator.range(0, indent).foreach(_ => out.print(indentText))
        out.print(value)

      case (Some((prevLine, Markup(prevIndent, _))), (currentLine, Markup(currentIndent, value))) if prevLine != currentLine =>
        if (prevIndent < currentIndent) out.print(':')
        Iterator.range(prevLine, currentLine).foreach(_ => out.println())
        Iterator.range(0, currentIndent).foreach(_ => out.print(indentText))
        out.write(value)

      case (Some((_, _: Expr)), (_, _: Open)) =>
        out.print('(')
      case (Some((_, _: Markup)), (_, _: Close)) =>
        out.print(')')
      case (Some((_, _: Open)), (_, _: Next)) =>
        log.debug("Ignore comma before param list")
      case (Some((_, _: Markup)), (_, _: Next)) =>
        out.print(',')
      case (Some((_, _: Open)), (_, Key(_, _, value))) =>
        out.print(value)
      case (Some((_, _: Open)), (_, Value(_, _, value))) =>
        out.print(value)
      case (Some((_, _: Key)), (_, Value(_, _, value))) =>
        out.print('=')
        out.print(value)
      case (_, (_, Markup(_, value))) =>
        out.print(' ')
        out.print(value)
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
