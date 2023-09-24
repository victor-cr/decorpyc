package com.codegans.decorpyc.ast

case class TranslateString(
                            override val attributes: Map[String, _],
                            override val fileName: String,
                            override val lineNum: Int,
                            line: String,
                            newFileName: String,
                            newLineNum: Int,
                            language: String,
                            newLine: String) extends ASTNode with Attributes

object TranslateString extends ASTNodeFactory[TranslateString] {
  val keyNewloc = "newloc"
  val keyOld = "old"
  val keyNew = "new"
  val keyLanguage = "language"

  override def apply(context: NodeContext, attributes: Map[String, _], fileName: String, lineNum: Int): TranslateString = {
    val newLoc = attributes(keyNewloc).asInstanceOf[List[_]]
    val line = attributes(keyOld).asInstanceOf[String]
    val newLine = attributes(keyNew).asInstanceOf[String]
    val language = attributes(keyLanguage).asInstanceOf[String]

    new TranslateString(attributes - keyNewloc - keyNew - keyOld - keyLanguage, fileName, lineNum, line, newLoc.head.asInstanceOf[String], newLoc(1).asInstanceOf[Int], language, newLine)
  }
}
