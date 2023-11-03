package uz.scala.aws.s3

import eu.timepit.refined.types.string.NonEmptyString

case class AWSConfig(
    accessKey: NonEmptyString,
    secretKey: NonEmptyString,
    serviceEndpoint: NonEmptyString,
    signingRegion: NonEmptyString,
    bucketName: NonEmptyString,
  )
