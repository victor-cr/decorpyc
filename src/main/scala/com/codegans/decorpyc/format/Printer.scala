package com.codegans.decorpyc.format

import com.codegans.decorpyc.ast.IfCondition.ConditionType
import com.codegans.decorpyc.ast._
import com.codegans.decorpyc.ast.atl._
import com.codegans.decorpyc.ast.sl._
import com.codegans.decorpyc.format.Printer.{indentText, log}
import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, File, FileWriter, PrintWriter}
import java.nio.charset.StandardCharsets
import scala.collection.mutable.ListBuffer


class Printer(layout: Layout) {
  private var currentPriority: Int = 0

  def start(body: Body): Unit = {
    val priorities = body.children.filter(_.isInstanceOf[Init]).map(_.asInstanceOf[Init]).map(_.priority).distinct
    val startLine = body.children.head.lineNum

    if (priorities.size == 1 && priorities.head != 0 && startLine > 1 && body.children.head.isInstanceOf[Init]) {
      val init = body.children.head.asInstanceOf[Init]
      if (init.children.head.lineNum == init.lineNum) {
        currentPriority = priorities.head
        layout.printKeyword(1, 0, "init", exclusive = true)
        layout.printExpr(1, 0, "offset")
        layout.printExpr(1, 0, "=")
        layout.printExpr(1, 0, currentPriority)
      }
    }

    body.children.foreach((node: Node) => write(node, 0))
  }

  def write(node: Node, indent: Int): Unit = node match {
    case Init(_, children@DebugInfo(_, childLine) :: _, _, initLine, p) if childLine > initLine =>
      layout.printKeyword(initLine, indent, "init", exclusive = true)
      if (currentPriority != p) {
        layout.printExpr(initLine, indent, p)
        currentPriority = p
      }
      children.foreach(write(_, indent + 1))
    case Init(_, children@(_: Python) :: _, _, initLine, _) =>
      layout.printKeyword(initLine, indent, "init", exclusive = true)
      children.foreach(write(_, indent))
    case Init(_, children, _, _, _) =>
      children.foreach(write(_, indent))

    case Define(_, _, expectedLine, prefix, variable, operator, index, maybeCode) =>
      layout.printKeyword(expectedLine, indent, "define", exclusive = true)
      layout.printExpr(expectedLine, indent, prefix.map(_ + '.' + variable).getOrElse(variable))
      index.foreach { case PyCode(PyExpr(expression), _) =>
        layout.printOpen(expectedLine, indent, "[")
          .printExpr(expectedLine, indent, expression)
          .printClose(expectedLine, indent, "]")
      }
      layout.printExpr(expectedLine, indent, operator)
      layout.printExpr(expectedLine, indent, maybeCode.map(_.source.expression).getOrElse("None"))

    case Default(_, _, expectedLine, prefix, variable, maybeCode) =>
      layout.printKeyword(expectedLine, indent, "default", exclusive = true)
      layout.printExpr(expectedLine, indent, prefix.map(_ + '.' + variable).getOrElse(variable))
      layout.printExpr(expectedLine, indent, "=")
      layout.printExpr(expectedLine, indent, maybeCode.map(_.source.expression).getOrElse("None"))

    case Label(_, Nil, _, _, labelName, _) =>
      log.debug("Ignore virtual label: {}", labelName)
    case Label(_, children, _, expectedLine, labelName, params) =>
      layout.printKeyword(expectedLine, indent, "label", exclusive = true)
      layout.printExpr(expectedLine, indent, labelName)
      params.foreach(layout.printArgs(expectedLine, indent, _))
      children.foreach((node: Node) => write(node, indent + 1))

    case UserStatement(_, children, _, expectedLine, text, _, _, _) =>
      layout.printExpr(expectedLine, indent, text)
      children.foreach((node: Node) => write(node, indent + 1))

    case Menu(_, children, _, expectedLine, NodeRef(Label(_, Nil, _, _, labelName, params)), caption, args, withA, set, itemArgs) =>
      layout.printKeyword(expectedLine, indent, "menu", exclusive = true)
      args.foreach(layout.printArgs(expectedLine, indent, _))
      layout.printExpr(expectedLine, indent, labelName)
      params.foreach(layout.printArgs(expectedLine, indent, _))
      set.foreach {
        case DebugPyExpr(e, _, lineNum, _) =>
          layout.printKeyword(lineNum, indent + 1, "set").printExpr(lineNum, indent + 1, e)
          caption.foreach(e => layout.printText(lineNum + 1, indent + 1, e))
        case PyExpr(e) =>
          layout.printKeyword(expectedLine, indent + 1, "set").printExpr(expectedLine, indent + 1, e)
          caption.foreach(e => layout.printText(expectedLine + 1, indent + 1, e))
      }
      children.foreach((node: Node) => write(node, indent + 1))
    case Menu(_, children, _, expectedLine, _, caption, args, _, _, _) =>
      layout.printKeyword(expectedLine, indent, "menu", exclusive = true)
      args.foreach(layout.printArgs(expectedLine, indent, _))
      children.foreach((node: Node) => write(node, indent + 1))

    case MenuItem(children, _, expectedLine, text, None, args, _, _) =>
      layout.printText(expectedLine, indent, text)
      args.foreach(layout.printArgs(expectedLine, indent, _))
      children.foreach((node: Node) => write(node, indent + 1))
    case MenuItem(children, _, expectedLine, text, Some(PyExpr(condition)), args, _, _) =>
      layout.printText(expectedLine, indent, text)
      args.foreach(layout.printArgs(expectedLine, indent, _))
      layout.printKeyword(expectedLine, indent, "if")
      layout.printExpr(expectedLine, indent, condition)
      children.foreach((node: Node) => write(node, indent + 1))

    case Python(_, _, _, _, None) =>
      log.warn("Ignore empty code block")
    case Python(_, _, expectedLine, _, Some(OnelinerPyCode(PyExpr(expression), _, _))) =>
      log.debug("Write one-liner Python instruction")
      layout.printKey(expectedLine, indent, "$")
      layout.printExpr(expectedLine, indent, expression)
    case Python(_, _, expectedLine, prefix, Some(BlockPyCode(_, mode, _, lines))) =>
      log.debug("Write Python code block")
      if (mode.isBlank || mode == "exec" || mode == "eval") {
        layout.printKeyword(expectedLine, indent, "python")
      } else {
        layout.printKeyword(expectedLine, indent, "python")
        layout.printExpr(expectedLine, indent, mode)
        prefix.foreach(layout.printExpr(expectedLine, indent, _))
      }
      lines.foreach { case (lineNum, code) => layout.printExpr(lineNum + expectedLine, indent + 1, code) }

    case Say(_, _, expectedLine, text, _, _, who, withA, maybeArgs, attrs, tempAttrs, false) =>
      who.foreach(layout.printExpr(expectedLine, indent, _))
      attrs.foreach(attr => layout.printExpr(expectedLine, indent, attr))
      if (tempAttrs.nonEmpty) layout.printExpr(expectedLine, indent, "@")
      tempAttrs.foreach(attr => layout.printExpr(expectedLine, indent, attr))
      layout.printText(expectedLine, indent, text)
      maybeArgs.foreach(layout.printArgs(expectedLine, indent, _))
      withA.foreach(e => layout.printKeyword(expectedLine, indent, "with").printExpr(expectedLine, indent, e))
    case Say(_, _, expectedLine, text, _, _, who, withA, maybeArgs, attrs, tempAttrs, true) =>
      who.foreach(layout.printExpr(expectedLine, indent, _))
      attrs.foreach(attr => layout.printExpr(expectedLine, indent, attr))
      if (tempAttrs.nonEmpty) layout.printExpr(expectedLine, indent, "@")
      tempAttrs.foreach(attr => layout.printExpr(expectedLine, indent, attr))
      layout.printMultilineText(expectedLine, indent, text)
      maybeArgs.foreach(layout.printArgs(expectedLine, indent, _))
      withA.foreach(e => layout.printKeyword(expectedLine, indent, "with").printExpr(expectedLine, indent, e))

    case Jump(_, _, expectedLine, false, PyExpr(target)) =>
      layout.printKeyword(expectedLine, indent, "jump", exclusive = true)
      layout.printExpr(expectedLine, indent, target)
    case Jump(_, _, expectedLine, true, PyExpr(target)) =>
      layout.printKeyword(expectedLine, indent, "jump", exclusive = true)
      layout.printExpr(expectedLine, indent, "expression")
      layout.printExpr(expectedLine, indent, target)

    case Call(_, _, expectedLine, PyExpr(label), false, maybeArgs) =>
      layout.printKeyword(expectedLine, indent, "call", exclusive = true)
      layout.printExpr(expectedLine, indent, label)
      maybeArgs.foreach(args => layout.printArgs(expectedLine, indent, args))
    case Call(_, _, expectedLine, PyExpr(label), true, maybeArgs) =>
      layout.printKeyword(expectedLine, indent, "call", exclusive = true)
      layout.printKeyword(expectedLine, indent, "expression")
      layout.printExpr(expectedLine, indent, label)
      maybeArgs.foreach(args => layout.printKeyword(expectedLine, indent, "pass").printArgs(expectedLine, indent, args))

    case Pass(_, _, expectedLine) =>
      layout.printKeyword(expectedLine, indent, "pass", exclusive = true)

    case While(_, children, _, expectedLine, maybeCondition) =>
      layout.printKeyword(expectedLine, indent, "while", exclusive = true)
      layout.printExpr(expectedLine, indent, maybeCondition.map(_.expression).getOrElse("True"))
      children.foreach((node: Node) => write(node, indent + 1))

    case If(_, children, _, expectedLine) => children.foreach((node: Node) => write(node, indent))

    case IfCondition(children, _, expectedLine, ConditionType.IF, condition, _, _) =>
      layout.printKeyword(expectedLine, indent, "if", exclusive = true)
      layout.printExpr(expectedLine, indent, condition.map(_.expression).getOrElse("True"))
      children.foreach((node: Node) => write(node, indent + 1))
    case IfCondition(children, _, expectedLine, ConditionType.ELIF, condition, _, _) =>
      layout.printKeyword(expectedLine, indent, "elif", exclusive = true)
      layout.printExpr(expectedLine, indent, condition.map(_.expression).getOrElse("True"))
      children.foreach((node: Node) => write(node, indent + 1))
    case IfCondition(children, _, expectedLine, ConditionType.ELSE, _, _, _) =>
      layout.printKeyword(expectedLine, indent, "else", exclusive = true)
      children.foreach((node: Node) => write(node, indent + 1))

    case Return(_, _, expectedLine, maybeCode) =>
      layout.printKeyword(expectedLine, indent, "return", exclusive = true)
      maybeCode.foreach(e => layout.printExpr(expectedLine, indent, e.expression))

    case With(_, _, expectedLine, None, None) =>
      layout.printKeyword(expectedLine, indent, "with")
    case With(_, _, expectedLine, Some(PyExpr(expression)), None) =>
      layout.printKeyword(expectedLine, indent, "with")
      layout.printExpr(expectedLine, indent, expression)
    case With(_, _, expectedLine, _, Some(PyExpr(expression))) =>
      log.debug("Ignore paired `with`")
    //      writeIndent(ident, expectedLine)
    //      out.print("with ")
    //      out.print(expression)

    case Image(_, atl, _, expectedLine, names, None) =>
      layout.printKeyword(expectedLine, indent, "image", exclusive = true)
      names.foreach(layout.printExpr(expectedLine, indent, _))
      atl.foreach(writeATL(_, indent, initial = true))
    case Image(_, atl, _, expectedLine, names, Some(OnelinerPyCode(PyExpr(expression), _, _))) =>
      layout.printKeyword(expectedLine, indent, "image", exclusive = true)
      names.foreach(layout.printExpr(expectedLine, indent, _))
      layout.printExpr(expectedLine, indent, "=")
      layout.printExpr(expectedLine, indent, expression)
      atl.foreach(writeATL(_, indent, initial = true))
    case Image(_, atl, _, expectedLine, names, Some(BlockPyCode(PyExpr(expression), _, _, lines))) =>
      layout.printKeyword(expectedLine, indent, "image", exclusive = true)
      names.foreach(layout.printExpr(expectedLine, indent, _))
      layout.printExpr(expectedLine, indent, "=")
      lines.foreach { case (line, text) => layout.printExpr(expectedLine + line, indent, text) }
      atl.foreach(writeATL(_, indent, initial = true))

    case Style(_, _, expectedLine, name, parent, _, _, variant, props, _) =>
      layout.printKeyword(expectedLine, indent, "style", exclusive = true)
      layout.printExpr(expectedLine, indent, name)
      parent.foreach { e =>
        layout.printKeyword(expectedLine, indent, "is")
        layout.printExpr(expectedLine, indent, e)
      }
      props.foreach { case (key, Some(PyExpr(value))) =>
        layout.printExpr(expectedLine, indent + 1, key)
        layout.printExpr(expectedLine, indent + 1, value)
      }

    case Scene(_, _, expectedLine, _, (head: List[String]) :: _) =>
      layout.printKeyword(expectedLine, indent, "scene", exclusive = true)
      head.foreach(layout.printExpr(expectedLine, indent, _))

    case Screen(_, _, expectedLine, SLScreen(_, children, _, _, name, _, _, params) :: Nil) =>
      layout.printKeyword(expectedLine, indent, "screen", exclusive = true)
      layout.printExpr(expectedLine, indent, name)
      params.foreach(layout.printArgs(expectedLine, indent, _))
      children.foreach((node: Node) => write(node, indent + 1))

    case SLDisplayable(_, children, _, expectedLine, name, positional, keyword) =>
      layout.printExpr(expectedLine, indent, name)
      positional.foreach { case Some(PyExpr(expression)) => layout.printExpr(expectedLine, indent + 1, expression) }
      keyword.foreach { case (key, Some(PyExpr(expression))) => layout.printExpr(expectedLine, indent + 1, key).printExpr(expectedLine, indent + 1, expression) }
      children.foreach((node: Node) => write(node, indent + 1))

    case SLIf(_, _, expectedLine, entries) =>
      entries.zipWithIndex.foreach {
        case ((expression, node :: Nil), 0) =>
          layout.printKeyword(expectedLine, indent, "if")
          layout.printExpr(expectedLine, indent, expression.map(_.expression).getOrElse("True"))
          write(node, indent + 1)
        case ((None, node :: Nil), _) =>
          layout.printKeyword(node.lineNum - 1, indent, "else")
          write(node, indent + 1)
        case ((Some(DebugPyExpr(expression, _, lineNum, _)), node :: Nil), _) =>
          layout.printKeyword(lineNum, indent, "elif")
          layout.printExpr(lineNum, indent, expression)
          write(node, indent + 1)
      }

    case SLFor(_, children, _, expectedLine, variable, Some(PyExpr(expression)), keywords) =>
      layout.printKeyword(expectedLine, indent, "for")
      layout.printExpr(expectedLine, indent, variable)
      layout.printKeyword(expectedLine, indent, "in")
      layout.printExpr(expectedLine, indent, expression)
      children.foreach(write(_, indent + 1))

    case SLUse(_, _, expectedLine, _, block, _, maybeArgs, target) =>
      layout.printKeyword(expectedLine, indent, "use")
      layout.printExpr(expectedLine, indent, target)
      maybeArgs.foreach(layout.printArgs(expectedLine, indent, _))
      block.foreach(write(_, indent + 1))

    case SLTransclude(_, _, expectedLine) =>
      layout.printKeyword(expectedLine, indent, "transclude")

    case SLDefault(_, _, expectedLine, variable, expression) =>
      layout.printKeyword(expectedLine, indent, "default")
      layout.printExpr(expectedLine, indent, variable)
      layout.printExpr(expectedLine, indent, "=")
      layout.printExpr(expectedLine, indent, expression.map(_.expression).getOrElse("None"))

    case SLPython(_, _, expectedLine, code) =>
      layout.printKeyword(expectedLine, indent, "python")

    case SLBlock(_, children, _, expectedLine, keyword) =>
      keyword.foreach { case (key, Some(PyExpr(expression))) => layout.printExpr(expectedLine, indent, key).printExpr(expectedLine, indent, expression) }
      children.foreach((node: Node) => write(node, indent))

    case Show(_, atl, _, expectedLine, (head: List[String]) :: _) =>
      layout.printKeyword(expectedLine, indent, "show")
      head.foreach(layout.printExpr(expectedLine, indent, _))
      atl.foreach(writeATL(_, indent, initial = true))

    case Hide(_, _, expectedLine, (head: List[String]) :: _) =>
      layout.printKeyword(expectedLine, indent, "hide")
      head.foreach(layout.printExpr(expectedLine, indent, _))

    case Translate(_, children, _, expectedLine, id, _, _) =>
      layout.printKeyword(expectedLine, indent, "translate", exclusive = true)
      layout.printExpr(expectedLine, indent, id)
      children.foreach((node: Node) => write(node, indent + 1))

    case Transform(_, atl, _, expectedLine, variable, params) =>
      layout.printKeyword(expectedLine, indent, "transform", exclusive = true)
      layout.printExpr(expectedLine, indent, variable)
      params.foreach(layout.printArgs(expectedLine, indent, _))
      atl.foreach(writeATL(_, indent, initial = true))

    case EndTranslate(_, _, expectedLine) =>
      layout.printKeyword(expectedLine, indent, "end translate")

  }

  def writeATL(node: ATLNode, indent: Int, initial: Boolean = false): Unit = node match {
    case ATLRawBlock(_, children, _, expectedLine, animation) if initial =>
      children.foreach(writeATL(_, indent + 1))
    case ATLRawBlock(_, children, _, expectedLine, animation) =>
      layout.printKeyword(expectedLine - 1, indent, "block", exclusive = true)
      children.foreach(writeATL(_, indent + 1))

    case ATLRawOn(_, children, _, expectedLine) =>
      children.foreach { case (key, ATLRawBlock(_, children, _, lineNum, _)) =>
        layout.printKeyword(lineNum - 1, indent, "on", exclusive = true)
        layout.printExpr(lineNum - 1, indent, key)
        children.foreach(writeATL(_, indent + 1))
      }
    case ATLRawChild(_, children, _, expectedLine) =>
      children.foreach(writeATL(_, indent))

    case ATLRawParallel(_, children, _, expectedLine) =>
      children.foreach { case ATLRawBlock(_, children, _, lineNum, _) =>
        layout.printKeyword(lineNum - 1, indent, "parallel", exclusive = true)
        children.foreach(writeATL(_, indent + 1))
      }

    case ATLRawFunction(_, _, expectedLine, Some(PyExpr(expression))) =>
      layout.printKeyword(expectedLine, indent, "function", exclusive = true)
      layout.printExpr(expectedLine, indent, expression)

    case ATLRawRepeat(_, _, expectedLine, maybeExpr) =>
      layout.printKeyword(expectedLine, indent, "repeat")
      maybeExpr.foreach(e => layout.printExpr(expectedLine, indent, e.expression))

    case ATLRawMultipurpose(_, _, expectedLine, maybeDuration, splines, maybeRevolution, _, maybeWarper, expressions, maybeCircles, props) =>
      maybeWarper.foreach { warp =>
        layout.printExpr(expectedLine, indent, warp)
        maybeDuration.foreach(e => layout.printExpr(expectedLine, indent, e.expression))
      }
      props.foreach { case (key, maybeExpr) =>
        layout.printExpr(expectedLine, indent, key)
        maybeExpr.foreach(e => layout.printExpr(expectedLine, indent, e.expression))
      }
      splines.foreach { case (key, value) =>
        layout.printExpr(expectedLine, indent, key)
        value.zipWithIndex.foreach {
          case (PyExpr(expression), 0) =>
            layout.printExpr(expectedLine, indent, expression)
          case (PyExpr(expression), _) =>
            layout.printExpr(expectedLine, indent, "knot").printExpr(expectedLine, indent, expression)
        }
      }
      maybeRevolution.foreach(e => layout.printExpr(expectedLine, indent, e))
      maybeCircles.foreach(e => layout.printKeyword(expectedLine, indent, "circles").printExpr(expectedLine, indent, e.expression))

      expressions.foreach {
        case (Some(DebugPyExpr(exprL, _, lineL, _)), Some(DebugPyExpr(exprR, _, lineR, _))) =>
          layout.printExpr(lineL, indent, exprL)
          layout.printKeyword(lineR, indent, "with")
          layout.printExpr(lineR, indent, exprR)
        case (Some(DebugPyExpr(exprL, _, lineL, _)), None) =>
          layout.printExpr(lineL, indent, exprL)
        case (None, Some(DebugPyExpr(exprR, _, lineR, _))) =>
          layout.printKeyword(lineR, indent, "with")
          layout.printExpr(lineR, indent, exprR)
      }
  }
}

object Printer {
  private val log: Logger = LoggerFactory.getLogger(classOf[Printer])
  private val indentChar = ' '
  private val indentSize = 4
  private val indentText = Iterator.fill(indentSize)(indentChar).mkString

  def toSource(root: Root): ByteSource = {
    val layout = new Layout

    new Printer(layout).start(root.body)
    new PrettyPrint(layout).printToByteArray(StandardCharsets.UTF_8)

    ByteSource(new PrettyPrint(layout).printToByteArray(StandardCharsets.UTF_8))
  }

  def write(file: File, root: Root): Unit = {
    val layout = new Layout

    new Printer(layout).start(root.body)
    new PrettyPrint(layout).printToByteArray(StandardCharsets.UTF_8)

    new PrettyPrint(layout).printToFile(file, StandardCharsets.UTF_8)
  }
}
