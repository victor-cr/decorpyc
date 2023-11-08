package com.codegans.decorpyc.transform

import com.codegans.decorpyc.ast
import com.codegans.decorpyc.ast._
import com.codegans.decorpyc.ast.atl._
import com.codegans.decorpyc.ast.sl._
import com.codegans.decorpyc.opcode._
import com.codegans.decorpyc.transform.OpcodeTransformer.defaultPyVersion

import scala.annotation.switch
import scala.collection.mutable.ListBuffer

class OpcodeTransformer(interceptor: NodeInterceptor) extends NodeContext with Function[OpcodeInstruction, Root] {
  private val instanceTable: ListBuffer[Option[Node]] = ListBuffer()

  override def ref(id: Int): NodeRef = new NodeRef(instanceTable(id).get)

  override def ref(instance: Any): NodeRef = instance match {
    case NewInstance(id, _, _) => ref(id)
    case node: Node => new NodeRef(node)
    case _ => throw new IllegalArgumentException(s"Unknown instance identifier: $instance")
  }

  override def transformAST: PartialFunction[Any, List[ASTNode]] = {
    case None => Nil
    case Some(value) => transformAST(value)
    case SetState(NewInstance(id, GlobalFunction("renpy.ast", className), Nil), (_: Map[Any, Any]) :: (attributes: Map[String, _]) :: Nil) =>
      val fileName = attributes("filename").asInstanceOf[String]
      val lineNum = attributes("linenumber").asInstanceOf[Int]
      List(storeInstance(id, transformAST(className, attributes - "filename" - "linenumber", fileName, lineNum)))
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.ast", className), Nil), attributes: Map[String, _]) =>
      val fileName = attributes("filename").asInstanceOf[String]
      val lineNum = attributes("linenumber").asInstanceOf[Int]
      List(storeInstance(id, transformAST(className, attributes - "filename" - "linenumber", fileName, lineNum)))
    case value => throw new IllegalArgumentException(s"Unknown AST instruction: $value")
  }

  override def transformATL: PartialFunction[Any, List[ATLNode]] = {
    case None => Nil
    case Some(value) => transformATL(value)
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.atl", className), Nil), attributes: Map[String, _]) =>
      val (fileName, lineNum) = attributes("loc") match {
        case (fileName: String) :: (lineNum: Int) :: Nil => fileName -> lineNum
        case value => throw new IllegalArgumentException(s"Location is not a valid list: $value")
      }
      List(storeInstance(id, transformATL(className, attributes - "loc", fileName, lineNum)))
    case value => throw new IllegalArgumentException(s"Unknown ATL instruction: $value")
  }

  override def transformSL: PartialFunction[Any, List[SLNode]] = {
    case None => Nil
    case Some(value) => transformSL(value)
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.sl2.slast", className), Nil), attributes: Map[String, _]) =>
      val (fileName, lineNum) = attributes("location") match {
        case (fileName: String) :: (lineNum: Int) :: Nil => fileName -> lineNum
        case value => throw new IllegalArgumentException(s"Location is not a valid list: $value")
      }
      List(storeInstance(id, transformSL(className, attributes - "location", fileName, lineNum)))
    case value => throw new IllegalArgumentException(s"Unknown SL instruction: $value")
  }

  override def transformPyExpr: PartialFunction[Any, Option[PyExpr]] = {
    case None => None
    case Some(value) => transformPyExpr(value)
    case str: String => Some(StringPyExpr(str))
    case num: Int => Some(StringPyExpr(num.toString))
    case code: PyCode => Some(code.source)
    case expr: PyExpr => Some(expr)
    case NewInstance(id, GlobalFunction("renpy.ast", "PyExpr"), (expression: String) :: ((fileName: String) :: (lineNum: Int) :: (py: Int) :: Nil) :: Nil) =>
      Some(storeInstance(id, DebugPyExpr(expression, fileName, lineNum, py)))
    case NewInstance(id, GlobalFunction("renpy.ast", "PyExpr"), (expression: String) :: ((fileName: String) :: (lineNum: Int) :: Nil) :: Nil) =>
      Some(storeInstance(id, DebugPyExpr(expression, fileName, lineNum, defaultPyVersion)))
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.ast", "PyExpr"), (expression: String) :: (_: List[_]) :: Nil), attributes: Map[String, _]) =>
      val fileName = attributes("filename").asInstanceOf[String]
      val lineNum = attributes("linenumber").asInstanceOf[Int]
      val py = attributes.get("py").map(_.asInstanceOf[Int]).getOrElse(defaultPyVersion)
      Some(storeInstance(id, DebugPyExpr(expression, fileName, lineNum, py)))
    case value => throw new IllegalArgumentException(s"Not valid PyExpr object: $value")
  }

  override def transformPyCode: PartialFunction[Any, Option[PyCode]] = {
    case None => None
    case Some(value) => transformPyCode(value)
    case code: PyCode => Some(code)
    case expr: DebugPyExpr => Some(PyCode(expr, "eval", expr.py))
    case expr: PyExpr => Some(PyCode(expr, "eval", defaultPyVersion))
    case SetState(NewInstance(id, GlobalFunction("renpy.ast", "PyCode"), Nil), 1 :: (source: String) :: ((fileName: String) :: (lineNum: Int) :: (_: Int) :: Nil) :: (mode: String) :: (py: Int) :: Nil) =>
      Some(storeInstance(id, PyCode(DebugPyExpr(source, fileName, lineNum, py), mode, py)))
    case SetState(NewInstance(id, GlobalFunction("renpy.ast", "PyCode"), Nil), 1 :: source :: (_: List[_]) :: (mode: String) :: (py: Int) :: Nil) =>
      transformPyExpr(source).map(expr => storeInstance(id, ast.PyCode(expr, mode, py)))
    case SetState(NewInstance(id, GlobalFunction("renpy.ast", "PyCode"), Nil), 1 :: source :: (_: List[_]) :: (mode: String) :: Nil) =>
      transformPyExpr(source).map(expr => storeInstance(id, ast.PyCode(expr, mode, defaultPyVersion)))
    case value => throw new IllegalArgumentException(s"Value is not a code block: $value")
  }

  override def transformArgumentInfo: PartialFunction[Any, Option[ArgumentInfo]] = {
    case None => None
    case Some(value) => transformArgumentInfo(value)
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.ast", "ArgumentInfo"), Nil), attributes: Map[String, _]) =>
      Some(storeInstance(id, transformArgumentInfo(attributes)))
    case value => throw new IllegalArgumentException(s"Not valid ArgumentInfo object: $value")
  }

  override def transformParameterInfo: PartialFunction[Any, Option[ParameterInfo]] = {
    case None => None
    case Some(value) => transformParameterInfo(value)
    case SetUpdate(NewInstance(id, GlobalFunction("renpy.ast", "ParameterInfo"), Nil), attributes: Map[String, _]) =>
      Some(storeInstance(id, transformParameterInfo(attributes)))
    case value => throw new IllegalArgumentException(s"Not valid ParameterInfo object: $value")
  }

  override def transformStringMap: PartialFunction[Any, Map[String, _]] = super.transformStringMap.orElse {
    case SetMapItem(_, key: String, value) => Map(key -> value)
    case value => throw new IllegalArgumentException(s"Not valid map object: $value")
  }

  override def transformString: PartialFunction[Any, Option[String]] = super.transformString.orElse {
    case value => throw new IllegalArgumentException(s"Not valid string object: $value")
  }

  override def transformList: PartialFunction[Any, List[String]] = super.transformStringList.orElse {
    case value => throw new IllegalArgumentException(s"Not valid generic list: $value")
  }

  override def transformStringList: PartialFunction[Any, List[String]] = super.transformStringList.orElse {
    case value => throw new IllegalArgumentException(s"Not valid string list: $value")
  }

  override def apply(root: OpcodeInstruction): Root = root match {
    case OpcodeRoot(attributes, list) =>
      val version = attributes("version").asInstanceOf[Int]
      val key = attributes("key").asInstanceOf[String]

      val nodes = list.flatMap(transformAST)

      val header = Header(attributes, version, key)
      val body = interceptor.replace(Body(nodes))

      Root(header, body)
  }

  private def transformArgumentInfo(attributes: Map[String, _]): ArgumentInfo = {
    val starredIndexes = attributes.getOrElse("starred_indexes", Nil)
    val doubleStarredIndexes = attributes.getOrElse("starred_indexes", Nil)
    val version = attributes.getOrElse("__version__", 0).asInstanceOf[Int]
    val args = attributes("arguments").asInstanceOf[List[_]].map {
      case None :: r :: Nil => None -> transformPyExpr(r)
      case (l: String) :: r :: Nil => Some(l) -> transformPyExpr(r)
    }

    ArgumentInfo(attributes - "starred_indexes" - "__version__" - "arguments", args, starredIndexes, doubleStarredIndexes, version)
  }

  private def transformParameterInfo(attributes: Map[String, _]): ParameterInfo = {
    val params = attributes("parameters").asInstanceOf[List[_]].map { case (l: String) :: r :: Nil => l -> transformPyExpr(r) }.toMap
    val extra = transformString(attributes("extrakw"))
    val extraPos = attributes("extrapos").asInstanceOf[Option[_]]
    val positional = transformStringList(attributes.get("positional")) // List of parameter names
    val positionalOnly = attributes.getOrElse("positional_only", Nil).asInstanceOf[List[_]]
    val keywordOnly = attributes.getOrElse("keyword_only", Nil).asInstanceOf[List[_]]

    ParameterInfo(attributes - "parameters", params)
  }

  private def transformAST(renpyType: String, attributes: Map[String, _], fileName: String, lineNum: Int): ASTNode = (renpyType: @switch) match {
    case "Init" => interceptor.replace(Init(this, attributes, fileName, lineNum))
    case "Label" => interceptor.replace(Label(this, attributes, fileName, lineNum))
    case "Return" => interceptor.replace(Return(this, attributes, fileName, lineNum))
    case "Jump" => interceptor.replace(Jump(this, attributes, fileName, lineNum))
    case "With" => interceptor.replace(With(this, attributes, fileName, lineNum))
    case "Scene" => interceptor.replace(Scene(this, attributes, fileName, lineNum))
    case "Screen" => interceptor.replace(Screen(this, attributes, fileName, lineNum))
    case "Show" => interceptor.replace(Show(this, attributes, fileName, lineNum))
    case "ShowLayer" => interceptor.replace(ShowLayer(this, attributes, fileName, lineNum))
    case "Hide" => interceptor.replace(Hide(this, attributes, fileName, lineNum))
    case "UserStatement" => interceptor.replace(UserStatement(this, attributes, fileName, lineNum))
    case "Menu" => interceptor.replace(Menu(this, attributes, fileName, lineNum))
    case "Say" => interceptor.replace(Say(this, attributes, fileName, lineNum))
    case "Translate" => interceptor.replace(Translate(this, attributes, fileName, lineNum))
    case "TranslateBlock" => interceptor.replace(TranslateBlock(this, attributes, fileName, lineNum))
    case "TranslateEarlyBlock" => interceptor.replace(TranslateBlock(this, attributes, fileName, lineNum))
    case "TranslateString" => interceptor.replace(TranslateString(this, attributes, fileName, lineNum))
    case "EndTranslate" => interceptor.replace(EndTranslate(this, attributes, fileName, lineNum))
    case "Call" => interceptor.replace(Call(this, attributes, fileName, lineNum))
    case "Pass" => interceptor.replace(Pass(this, attributes, fileName, lineNum))
    case "Define" => interceptor.replace(Define(this, attributes, fileName, lineNum))
    case "Default" => interceptor.replace(Default(this, attributes, fileName, lineNum))
    case "Transform" => interceptor.replace(Transform(this, attributes, fileName, lineNum))
    case "Image" => interceptor.replace(Image(this, attributes, fileName, lineNum))
    case "Style" => interceptor.replace(Style(this, attributes, fileName, lineNum))
    case "Python" => interceptor.replace(Python(this, attributes, fileName, lineNum))
    case "If" => interceptor.replace(If(this, attributes, fileName, lineNum))
    case "While" => interceptor.replace(While(this, attributes, fileName, lineNum))
  }

  private def transformATL(renpyType: String, attributes: Map[String, _], fileName: String, lineNum: Int): ATLNode = (renpyType: @switch) match {
    case "RawBlock" => interceptor.replace(ATLRawBlock(this, attributes, fileName, lineNum))
    case "RawChild" => interceptor.replace(ATLRawChild(this, attributes, fileName, lineNum))
    case "RawChoice" => interceptor.replace(ATLRawChoice(this, attributes, fileName, lineNum))
    case "RawRepeat" => interceptor.replace(ATLRawRepeat(this, attributes, fileName, lineNum))
    case "RawFunction" => interceptor.replace(ATLRawFunction(this, attributes, fileName, lineNum))
    case "RawOn" => interceptor.replace(ATLRawOn(this, attributes, fileName, lineNum))
    case "RawMultipurpose" => interceptor.replace(ATLRawMultipurpose(this, attributes, fileName, lineNum))
    case "RawParallel" => interceptor.replace(ATLRawParallel(this, attributes, fileName, lineNum))
  }

  private def transformSL(renpyType: String, attributes: Map[String, _], fileName: String, lineNum: Int): SLNode = (renpyType: @switch) match {
    case "SLScreen" => interceptor.replace(SLScreen(this, attributes, fileName, lineNum))
    case "SLDisplayable" => interceptor.replace(SLDisplayable(this, attributes, fileName, lineNum))
    case "SLUse" => interceptor.replace(SLUse(this, attributes, fileName, lineNum))
    case "SLTransclude" => interceptor.replace(SLTransclude(this, attributes, fileName, lineNum))
    case "SLDefault" => interceptor.replace(SLDefault(this, attributes, fileName, lineNum))
    case "SLPython" => interceptor.replace(SLPython(this, attributes, fileName, lineNum))
    case "SLIf" => interceptor.replace(SLIf(this, attributes, fileName, lineNum))
    case "SLShowIf" => interceptor.replace(SLIf(this, attributes, fileName, lineNum))
    case "SLFor" => interceptor.replace(SLFor(this, attributes, fileName, lineNum))
    case "SLBlock" => interceptor.replace(SLBlock(this, attributes, fileName, lineNum))
  }

  private def storeInstance[T <: Node](id: Int, instance: T): T = {
    val size = instanceTable.size

    if (id > size) {
      Iterator.range(size, id).foreach(_ => instanceTable.addOne(None))
      instanceTable.addOne(Some(instance))
    } else if (id < size) {
      instanceTable.update(id, Some(instance))
    } else {
      instanceTable.addOne(Some(instance))
    }

    instance
  }
}

object OpcodeTransformer {
  private val defaultPyVersion: Int = 0

}
