package com.codegans.decorpyc

import com.codegans.decorpyc.DecompilerSpec.{genericTests, readSource, renpyFilter, versionDirs, versionTests}
import com.codegans.decorpyc.file.FileInfo
import com.codegans.decorpyc.util.ByteSource
import org.scalatest.Inspectors.forAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.io.{BufferedInputStream, File, FileFilter, FileInputStream, FilenameFilter}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.regex.Pattern
import scala.io.Source

class DecompilerSpec extends AnyFreeSpec {
  forAll(versionDirs) { versionDir =>
    s"Decompiled binaries of ${versionDir.getName}" - {

      forAll(genericTests) { case (dir, testFileName) =>
        s"match generic source `$testFileName`" in {
          val fileInfo = FileInfo(s"${testFileName}c", ByteSource(new File(versionDir, s"generic/${testFileName}c")))
          val actual = new String(fileInfo.decompiled.toArray, StandardCharsets.UTF_8)
          val expected = readSource(dir, testFileName, StandardCharsets.UTF_8)

          actual shouldEqual expected
        }
      }

      forAll(versionTests(versionDir)) { testFileName =>
        s"match version-specific source `${versionDir.getName}/$testFileName`" in {
          val fileInfo = FileInfo(s"${testFileName}c", ByteSource(new File(versionDir, s"${testFileName}c")))
          val actual = new String(fileInfo.decompiled.toArray, StandardCharsets.UTF_8)
          val expected = readSource(versionDir, testFileName, StandardCharsets.UTF_8)

          actual shouldEqual expected
        }
      }
    }
  }
}

object DecompilerSpec {
  private val renpyFilter: FileFilter = (file: File) => file.isFile && file.getName.endsWith(".rpy")
  private val versionFilter: FileFilter = (file: File) => file.isDirectory && file.getName.matches("^v\\d\\.\\d$")

  private val genericDir: File = new File(this.getClass.getClassLoader.getResource("generic").toURI).getAbsoluteFile.getCanonicalFile
  private val rootDir: File = genericDir.getParentFile.getAbsoluteFile.getCanonicalFile
  //  private val versionDirs: List[File] = List(new File(rootDir, "v7.3").getAbsoluteFile.getCanonicalFile) // rootDir.listFiles(versionFilter).map(_.getAbsoluteFile.getCanonicalFile).toList
  private val versionDirs: List[File] = rootDir.listFiles(versionFilter).map(_.getAbsoluteFile.getCanonicalFile).toList

  private val genericTests: List[(File, String)] = genericDir.listFiles(renpyFilter).map(file => genericDir -> relativize(genericDir, file)).toList
  private val versionTests: Map[File, List[String]] = versionDirs.map(dir => dir -> dir.listFiles(renpyFilter).map(file => relativize(dir, file)).toList).toMap

  private def relativize(parent: File, child: File): String = child.getAbsolutePath.stripPrefix(parent.getAbsolutePath).substring(1).replace('\\', '/')

  private def readSource(parent: File, name: String, charset: Charset): String = {
    val source = Source.fromFile(new File(parent, name), charset.name())
    try {
      source.getLines().mkString(System.lineSeparator())
        .replaceAll("(?m)^[ ]*(?:#.*)?$", "")
        .replaceAll("(?m)^(.*?)[ ]+(#.*)$", "$1")
      //          .replaceAll("init offset = -2", "")

    } finally {
      source.close()
    }
  }
}
