import sbtrelease.{Version, versionFormatError}

object Release {
  private type TaskImplementation = String => String

  def fnReleaseVersion: TaskImplementation = { version =>
    Version(version).map(_.withoutQualifier.string + "-RELEASE").getOrElse(versionFormatError(version))
  }

  def fnNextReleaseVersion(releaseVersionBump: Version.Bump): TaskImplementation = { version =>
    Version(version).map(_.bump(releaseVersionBump).asSnapshot.string).getOrElse(versionFormatError(version))
  }

//  releaseProcess := Seq[ReleaseStep](
//    checkSnapshotDependencies, // : ReleaseStep
//    inquireVersions, // : ReleaseStep
//    runClean, // : ReleaseStep
//    runTest, // : ReleaseStep
//    setReleaseVersion, // : ReleaseStep
//    commitReleaseVersion, // : ReleaseStep, performs the initial git checks
//    tagRelease, // : ReleaseStep
//    publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
//    setNextVersion, // : ReleaseStep
//    commitNextVersion, // : ReleaseStep
//    pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
//  )

}
