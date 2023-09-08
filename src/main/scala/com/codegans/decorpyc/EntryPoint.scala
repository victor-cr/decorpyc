package com.codegans.decorpyc

import com.codegans.decorpyc.file.{ArchiveInfo, FileInfo}
import com.codegans.decorpyc.util.ByteSource
import org.rogach.scallop.{Compat, Scallop, ScallopConf, ScallopOption}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import scala.collection.mutable.ListBuffer

object EntryPoint {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  private lazy val artifact: String = Option(getClass.getPackage.getImplementationTitle).getOrElse("decorpyc")
  private lazy val version: String = Option(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-DEBUG")
  private val banner: String =
    """
      | #######################################################################################
      | ## Decompiler for Ren'Py binaries.                                                   ##
      | #######################################################################################
      |
      |""".stripMargin

  final def main(args: Array[String]): Unit = {
    val config = new Conf(args)

    val output = config.output()

    val inputFiles: ListBuffer[File] = ListBuffer()

    config.input().foreach { input =>
      if (input.isFile) {
        log.info("Prepare to decompile the file: {}", input.getAbsolutePath)
        inputFiles.addOne(input)
      } else if (input.isDirectory) {
        log.info("Prepare to recursively search the directory: {}", input.getAbsolutePath)
        val fs = FileSystems.getDefault

        val visitor = new FileFinder(fs.getPathMatcher("glob:*.{rpyc,rpa}"))

        Files.walkFileTree(input.toPath, visitor)

        inputFiles.addAll(visitor.files)
      } else {
        log.warn("Cannot locate neither directory nor file: {}", input.getAbsolutePath)
        return
      }
    }

    if (inputFiles.isEmpty) {
      log.warn("Cannot locate neither RenPy compiled files nor archives: {}", config.input())
      return
    }

    val archives = inputFiles.filter(_.getName.endsWith(".rpa"))
    val binaries = inputFiles.filter(_.getName.endsWith(".rpyc"))

    if (output.isDirectory) {
      log.debug("Output directory has already been created: {}", output.getAbsolutePath)
    } else if (!output.exists()) {
      log.debug("Missing the output directory: {}", output.getAbsolutePath)
      output.mkdirs()
    }

    if (!output.isDirectory) {
      log.warn("Looks like the output either is not a directory or cannot be created: {}", output.getAbsolutePath)
      return
    }

    archives.foreach { file =>
      val source = ByteSource(file)

      val archiveInfo = ArchiveInfo(source)

      archiveInfo.files.foreach { fileInfo =>
        val newName = if (fileInfo.name.endsWith(".rpyc")) fileInfo.name.stripSuffix("c") else fileInfo.name

        val result = new File(output, newName)

        result.getParentFile.mkdirs()

        fileInfo.decompiled.writeTo(result)

        if (newName != fileInfo.name) {
          log.info("Successfully wrote decompiled results to: {}", result.getAbsolutePath)
        } else {
          log.info("Successfully wrote extracted resource to: {}", result.getAbsolutePath)
        }
      }
    }

    binaries.foreach { file =>
      val source = ByteSource(file)

      val fileInfo = FileInfo(file.getName, source)

      val newName = if (fileInfo.name.endsWith(".rpyc")) fileInfo.name.stripSuffix("c") else fileInfo.name

      val result = new File(output, newName)

      result.getParentFile.mkdirs()

      fileInfo.decompiled.writeTo(result)

      log.info("Successfully wrote decompiled results to: {}", result.getAbsolutePath)
    }
  }

  private class FileFinder(matcher: PathMatcher) extends SimpleFileVisitor[Path] {
    val files: ListBuffer[File] = ListBuffer()

    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
      log.debug("Visiting directory: {}", dir)
      super.preVisitDirectory(dir, attrs)
    }

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (matcher.matches(file)) files.addOne(file.toFile)
      super.visitFile(file, attrs)
    }

    override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
      log.error("Visiting file failed: {}", file, exc)
      super.visitFileFailed(file, exc)
    }

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      if (exc != null) log.error("Visiting directory failed: {}", dir, exc)
      super.postVisitDirectory(dir, exc)
    }
  }

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val help: ScallopOption[Boolean] = opt[Boolean](name = "help", short = '?', argName = "", descr = "Prints this help message and exits")
    val version: ScallopOption[Boolean] = opt[Boolean](name = "version", short = 'v', argName = "", descr = "Prints application version and exits")
    val output: ScallopOption[File] = opt[File](name = "output", short = 'o', descr = "Output directory for unpacked/decompiled files", argName = "dir", required = true)
    val input: ScallopOption[List[File]] = trailArg[List[File]](descr = "Directories or files which has to be unpacked/decompiled", required = true)

    version(EntryPoint.artifact + "-" + EntryPoint.version)
    banner(EntryPoint.banner)
    exitHandler = exitCode => {
      if (exitCode != 0) println(s"For help use `--${help.name}` start argument")
      Compat.exit(exitCode)
    }
    verify()
  }
}
