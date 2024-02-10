package com.codegans.decorpyc

import com.codegans.decorpyc.file.{ArchiveInfo, FileInfo}
import com.codegans.decorpyc.util.ByteSource
import org.rogach.scallop.{Compat, ScallopConf, ScallopOption}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
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
    val fs = FileSystems.getDefault
    val config = new Conf(args)

    val root = config.rootDir().getAbsoluteFile.getCanonicalFile.toPath
    val input = root.resolve(config.gameDir().toPath).toFile.getAbsoluteFile.getCanonicalFile
    val output = root.resolve(config.outputDir().toPath).toFile.getAbsoluteFile.getCanonicalFile
    val globPattern = if (config.scriptOnly()) "**/*.{rpy,rpyc,py}" else config.filter.map(_.mkString(",")).getOrElse("*.*")
    val globFilter = fs.getPathMatcher("glob:" + globPattern)

    log.info("Searching for unpacked resources at {}", input)

    val visitor = new FileFinder(fs.getPathMatcher("glob:**/*.{rpyc,rpy,rpa,rpi,py}"))

    Files.walkFileTree(input.toPath, visitor)

    val inputFiles: ListBuffer[File] = visitor.files

    if (inputFiles.isEmpty) {
      log.warn("Cannot locate neither RenPy compiled files nor archives: {}", input)
      return
    } else {
      log.info("Found {} files. Start processing.", inputFiles.size)
    }

    val archives = inputFiles.filter(e => e.getName.endsWith(".rpa") || e.getName.endsWith(".rpi"))
    val binaries = inputFiles.filter(_.getName.endsWith(".rpyc"))
    val sources = inputFiles.filter(e => e.getName.endsWith(".rpy") || e.getName.endsWith(".py"))

    if (output.isDirectory) {
      log.info("Output directory has already been created: {}", output.getAbsolutePath)
    } else if (!output.exists()) {
      log.info("Creating the output directory: {}", output.getAbsolutePath)
      output.mkdirs()
    }

    if (!output.isDirectory) {
      log.warn("Looks like the output either is not a directory or cannot be created: {}", output.getAbsolutePath)
      return
    }

    archives.foreach { file =>
      val source = ByteSource(file)

      val archiveInfo = ArchiveInfo(file.getName, source)

      archiveInfo.index.entries.foreach { entry =>
        val resource = new File(output, entry.name).getAbsoluteFile.getCanonicalFile

        if (globFilter.matches(resource.toPath)) {
          source.seek(entry.offset)

          resource.getParentFile.mkdirs()

          FileInfo(entry.name, source.read(entry.length)).decompiled.writeTo(resource)

          if (resource.getName.endsWith(".rpyc")) {
            log.info("Successfully wrote decompiled results to: {}", resource.getAbsolutePath)
          } else {
            log.info("Successfully wrote extracted resource to: {}", resource.getAbsolutePath)
          }
        }
      }
    }

    binaries.foreach { file =>
      val resource = output.toPath.resolve(input.toPath.relativize(file.toPath)).toFile

      if (globFilter.matches(resource.toPath)) {
        val source = ByteSource(file)

        val fileInfo = FileInfo(file.getName, source)

        resource.getParentFile.mkdirs()

        fileInfo.decompiled.writeTo(resource)

        log.info("Successfully wrote decompiled results to: {}", resource.getAbsolutePath)
      }
    }

    sources.foreach { file =>
      log.info("Do nothing: {}", file)
    }
  }

  private class FileFinder(matcher: PathMatcher) extends SimpleFileVisitor[Path] {
    val files: ListBuffer[File] = ListBuffer()

    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
      log.debug("Visiting directory: {}", dir)
      super.preVisitDirectory(dir, attrs)
    }

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (matcher.matches(file)) {
        log.info("Found file: {}", file)
        files.addOne(file.toFile)
      }
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
    val help: ScallopOption[Boolean] = opt[Boolean](
      name = "help", short = '?', argName = "",
      descr = "Prints this help message and exits"
    )
    val version: ScallopOption[Boolean] = opt[Boolean](
      name = "version", short = 'v', argName = "",
      descr = "Prints application version and exits"
    )
    val scriptOnly: ScallopOption[Boolean] = opt[Boolean](
      name = "script-only", short = 's', argName = "",
      descr = "Unpack decompile only Ren'Py scripts. Shortcut for `-f '**/*.rpy,**/*.rpyc,**/*.py'`"
    )
    val filter: ScallopOption[List[String]] = opt[List[String]](
      name = "filter", short = 'f', argName = "pattern",
      descr = "Comma-separated patterns to filter unpacked/decompiled files. See details about the pattern format at `https://ant.apache.org/manual/dirtasks.html`"
    )
    val gameDir: ScallopOption[File] = opt[File](
      name = "game", short = 'g', argName = "dir", default = Some(new File(".", "game")),
      descr = "Game directory where game files are located: compiled sources, RPA, etc. Default is `game`."
    )
    val outputDir: ScallopOption[File] = opt[File](
      name = "output", short = 'o', argName = "dir", default = Some(new File(".", "output")),
      descr = "Output directory for unpacked/decompiled files. Default is `output`."
    )
    val rootDir: ScallopOption[File] = trailArg[File](
      descr = "Path to the directory of the Ren'Py game where executable/startup files are located."
    )

    version(EntryPoint.artifact + "-" + EntryPoint.version)
    banner(EntryPoint.banner)
    exitHandler = exitCode => {
      if (exitCode != 0) println(s"For help use `--${help.name}` start argument")
      Compat.exit(exitCode)
    }
    verify()
  }
}
