package com.codegans.decorpyc

import com.codegans.decorpyc.file.{ArchiveInfo, FileInfo}
import com.codegans.decorpyc.util.ByteSource
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileSystems, FileVisitResult, Files, Path, PathMatcher, SimpleFileVisitor}
import scala.collection.mutable.ListBuffer

object EntryPoint {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  final def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      log.debug("Invalid number of input arguments. Render help...")
      printHelp()
      return
    }

    val paths = args.map(arg => new File(arg).getAbsoluteFile.getCanonicalFile).toList

    val input = paths.head
    val output = paths(1)

    val inputFiles: ListBuffer[File] = ListBuffer()

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

    if (inputFiles.isEmpty) {
      log.warn("Cannot locate neither RenPy compiled files nor archives: {}", input.getAbsolutePath)
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
      }
    }

    binaries.foreach { file =>
      val source = ByteSource(file)

      val fileInfo = FileInfo(file.getName, source)

      val newName = if (fileInfo.name.endsWith(".rpyc")) fileInfo.name.stripSuffix("c") else fileInfo.name

      val result = new File(output, newName)

      result.getParentFile.mkdirs()

      fileInfo.decompiled.writeTo(result)
    }
  }

  private def printHelp(): Unit = {
    println("###############################################")
    println("Usage:")
    println(">> java -jar decorpyc-<version>.jar <input directory/file> <output directory>")
    println("###############################################")
    println("Example:")
    println(">> java -jar decorpyc-0.1.0.jar 'MyCoolRenPy/game' 'MyCoolRenPy/output'")
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
}
