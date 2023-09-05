import sbt.{Def, *}
import sbt.Keys.*
import sbt.internal.util.ManagedLogger

import scala.sys.process.*

object RenPyPlugin extends AutoPlugin {
  private val versions: Seq[String] = Seq("8.1.1", "8.0.3", "7.6.1", "7.5.3", "7.4.11", "7.3.5")
  private val resourceDir = Test / resourceDirectory
  private val targetDir = target

  override def requires: Plugins = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val renpyVersions: SettingKey[Seq[String]] = settingKey[Seq[String]]("Supported version of Ren'Py SDK")
    val renpyDownload: TaskKey[Unit] = taskKey[Unit]("Download of all supported major versions of Ren'Py SDK")
    val renpyCompile: TaskKey[Unit] = taskKey[Unit]("Compile test sources with all supported major versions of Ren'Py SDK")

    lazy val renpySettings: Seq[Def.Setting[_]] = Seq(
      renpyVersions := versions,
      renpyDownload := {
        DownloadRenPySettings(streams.value.log, renpyVersions.value, managedDirectory.value)
      },
      renpyCompile := {
        CompileRenPySettings(streams.value.log, renpyVersions.value, managedDirectory.value, (Test / resourceDirectory).value, target.value / "renpy")
      }
    )
  }

  import autoImport._

  override val projectSettings: Seq[Def.Setting[_]] = inConfig(Compile)(renpySettings)

  object DownloadRenPySettings {
    def apply(log: ManagedLogger, versionsRenPy: Seq[String], renpySdkCacheDirectory: File): Unit = {
      versionsRenPy.foreach { version =>
        val packageName = s"renpy-$version-sdk"
        val outputDir = renpySdkCacheDirectory / packageName
        val tmpFile = renpySdkCacheDirectory / s"$packageName.tmp"
        val zipFile = renpySdkCacheDirectory / s"$packageName.zip"

        if (zipFile.isFile && outputDir.isDirectory) {
          log.info(s"Ren'Py v$version has been already downloaded. Skip...")
        } else {
          if (tmpFile.isFile) {
            log.warn(s"Previous Ren'Py v$version download attempt has been failed. Started it over...")
            IO.delete(tmpFile)
          }

          if (zipFile.isFile) {
            log.info(s"Ren'Py v$version has been already downloaded, but not unzipped.")
          } else {
            log.info(s"Downloading Ren'Py v$version...")
            url(s"https://www.renpy.org/dl/$version/renpy-$version-sdk.zip") #> tmpFile ! log
            log.info(s"Ren'Py v$version has been already downloaded to a temp file. Renaming...")
            IO.move(tmpFile, zipFile)
          }

          log.info(s"Unzipping Ren'Py version $version into temp directory")
          IO.unzip(zipFile, renpySdkCacheDirectory)
          log.info(s"Successfully installed Ren'Py v$version.")
        }
      }
    }
  }

  object CompileRenPySettings {
    def apply(log: ManagedLogger, versions: Seq[String], renpySdkCacheDirectory: File, resourcesRenPyDir: File, outputRenPyDir: File): Unit = {
      val genericSourceDir = resourcesRenPyDir / "generic"
      val genericSourceFiles: Seq[File] = (genericSourceDir ** "*.rpy").get()

      IO.delete(outputRenPyDir)
      IO.createDirectory(outputRenPyDir)

      log.info(s"Cleaned up target dir: $outputRenPyDir")

      versions.map(e => e -> e.substring(0, e.lastIndexOf('.'))).foreach { case (version, shortVersion) =>
        val sdkRoot = renpySdkCacheDirectory / s"renpy-$version-sdk"
        val launcher = sdkRoot / "launcher"
        val executable = System.getProperty("os.name") match {
          case win if win.contains(win) =>
            (sdkRoot / "renpy.exe").getAbsolutePath
          case _ if shortVersion.startsWith("8.") =>
            "py -3 " + (sdkRoot / "renpy.py").getAbsolutePath
          case _ =>
            "py -2 " + (sdkRoot / "renpy.py").getAbsolutePath
        }

        val sourceDir = resourcesRenPyDir / s"v$shortVersion"
        val targetDir = outputRenPyDir / s"v$shortVersion"
        val archiveDir = outputRenPyDir / "archive"

        val specificSourceFiles: Seq[File] = (sourceDir ** "*.rpy").get()

        val specificTargetFiles: Seq[File] = specificSourceFiles.flatMap(_.relativeTo(sourceDir)).map(f => new File(targetDir, f.getPath))
        val genericTargetFiles: Seq[File] = genericSourceFiles.flatMap(_.relativeTo(resourcesRenPyDir)).map(f => new File(targetDir, f.getPath))

        IO.copy(genericSourceFiles.zip(genericTargetFiles), overwrite = true, preserveLastModified = true, preserveExecutable = false)
        log.info(s"Copied generic sources into $targetDir")
        IO.copy(specificSourceFiles.zip(specificTargetFiles), overwrite = true, preserveLastModified = true, preserveExecutable = false)
        log.info(s"Copied version $version specific sources into $targetDir")

        s"$executable ${launcher.getAbsolutePath} distribute ${targetDir.getAbsolutePath} --package win" ! log
        log.info(s"Compiled sources with Ren'Py v$version")
        IO.unzip(archiveDir / s"decorpyc-$shortVersion-win.zip", archiveDir, NameFilter.fnToNameFilter(f => f.endsWith(".rpa")))
        log.info(s"Extracted version $version specific RPA into $archiveDir")

        val rpaArchiveFiles = (archiveDir / s"decorpyc-$shortVersion-win" ** "*.rpa").get()
        val rpaSourceFiles = rpaArchiveFiles.map(f => new File(sourceDir, f.getName))
        IO.copy(rpaArchiveFiles.zip(rpaSourceFiles), overwrite = true, preserveLastModified = true, preserveExecutable = false)
        log.info(s"Copied version $version specific RPA into $sourceDir")
      }

      val compiledTargetFiles: Seq[File] = (outputRenPyDir ** "*.rpyc").get()
      val compiledSourceFiles: Seq[File] = compiledTargetFiles.flatMap(_.relativeTo(outputRenPyDir)).map(f => new File(resourcesRenPyDir, f.getPath))

      IO.copy(compiledTargetFiles.zip(compiledSourceFiles), overwrite = true, preserveLastModified = true, preserveExecutable = false)
      log.warn(s"Copied compilation results back to test sources. WARNING!!! You need to manually add/remove files according to VCS rules.")
    }
  }
}
