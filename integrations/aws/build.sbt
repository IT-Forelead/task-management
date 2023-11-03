name := "aws"

lazy val `integration_aws-s3` = project.in(file("s3"))

aggregateProjects(
  `integration_aws-s3`
)
