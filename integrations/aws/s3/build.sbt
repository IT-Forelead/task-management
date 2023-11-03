name := "s3"

libraryDependencies ++=
  Dependencies.com.amazonaws.all ++
    Dependencies.co.fs2.all ++
    Seq(
      Dependencies.com.google.guava
    )

dependsOn(LocalProject("common"))
