package uz.scala.aws.s3

import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

import scala.jdk.CollectionConverters._

import cats.effect.Async
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxFlatMapOps
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer._
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import fs2._
import uz.scala.syntax.refined._
trait S3Client[F[_]] {
  private[this] val defaultChunkSize = 5 * 1024 * 1024

  def listFiles: Stream[F, String]

  def listBuckets: Stream[F, Bucket]

  def downloadObject(key: String): Stream[F, Byte]

  def deleteObject(key: String): Stream[F, Unit]

  def putObject(key: String, public: Boolean): Pipe[F, Byte, Unit]

  def uploadFileMultipart(key: String, chunkSize: Int = defaultChunkSize): Pipe[F, Byte, String]

  def objectUrl(key: String): F[URL]
}

object S3Client {
  private[this] def makeAmazonS3(awsConfig: AWSConfig): AmazonS3 = {
    val credentials = new BasicAWSCredentials(awsConfig.accessKey.value, awsConfig.secretKey.value)
    val credentialsProvider = new AWSStaticCredentialsProvider(credentials)
    AmazonS3ClientBuilder
      .standard()
      .withEndpointConfiguration(
        new EndpointConfiguration(awsConfig.serviceEndpoint, awsConfig.signingRegion)
      )
      .withCredentials(credentialsProvider)
      .build()
  }

  def resource[F[_]: Async](awsConfig: AWSConfig): Resource[F, S3Client[F]] =
    for {
      transferManager <- Resource.make(acquireTransferManager[F](awsConfig))(shutdown[F])
    } yield new S3ClientImpl[F](awsConfig, transferManager)

  private def acquireTransferManager[F[_]](
      awsConfig: AWSConfig
    )(implicit
      F: Sync[F]
    ): F[TransferManager] =
    F.delay(TransferManagerBuilder.standard().withS3Client(makeAmazonS3(awsConfig)).build())

  private def shutdown[F[_]](tm: TransferManager)(implicit F: Sync[F]): F[Unit] =
    F.delay(tm.shutdownNow())

  private class S3ClientImpl[F[_]: Async] private[s3] (
      awsConfig: AWSConfig,
      transferManager: TransferManager,
    )(implicit
      F: Sync[F]
    ) extends S3Client[F] {
    private[this] def makeMetadata(contentLength: Long): ObjectMetadata = {
      val metadata = new ObjectMetadata()
      metadata.setContentLength(contentLength)
      metadata
    }

    private def expireTime(): Date =
      Date.from(LocalDateTime.now().plusMinutes(60).atZone(ZoneId.systemDefault).toInstant)

    /** Uploads a file in a single request. Suitable for small files.
      *
      * For big files, consider using [[uploadFileMultipart]] instead.
      */

    override def putObject(key: String, public: Boolean): Pipe[F, Byte, Unit] =
      (s: Stream[F, Byte]) =>
        for {
          is <- s.through(io.toInputStream)
          bytes = ByteStreams.toByteArray(is)
          _ = is.close()
          byteSource = ByteSource.wrap(bytes)
          _ = transferManager.getAmazonS3Client.putObject {
            val uploadRequest =
              new PutObjectRequest(
                awsConfig.bucketName,
                key,
                byteSource.openStream(),
                makeMetadata(byteSource.size()),
              )
            uploadRequest.getRequestClientOptions.setReadLimit(1024 * 1024 * 5)
            if (public) {
              val acl = new AccessControlList()
              acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)
              uploadRequest.withAccessControlList(acl)
            }
            uploadRequest
          }
        } yield ()

    /** <p>Uploads a file in multiple parts of the specified <b color="yellow">partSize</b> per request. Suitable for
      * big files.</p>
      *
      * It does so in constant memory. So at a given time, only the number of bytes indicated by @partSize will be
      * loaded in memory.
      *
      * For small files, consider using [[putObject]] instead.
      *
      * @param chunkSize
      *   the part size indicated in MBs. It must be at least <b color="green">5</b>, as required by AWS.
      */

    override def uploadFileMultipart(
        key: String,
        chunkSize: Int,
      ): Pipe[F, Byte, String] = {

      val initiateMultipartUpload: F[String] =
        F.delay(
          transferManager
            .getAmazonS3Client
            .initiateMultipartUpload(
              new InitiateMultipartUploadRequest(awsConfig.bucketName, key)
            )
            .getUploadId
        )

      def uploadPart(uploadId: String): Pipe[F, (Chunk[Byte], Int), PartETag] =
        _.flatMap {
          case (c, i) =>
            for {
              is <- fs2.Stream.chunk(c).through(io.toInputStream)
              partReq = transferManager.getAmazonS3Client.uploadPart {
                val uploadPartRequest = new UploadPartRequest()
                uploadPartRequest.withBucketName(awsConfig.bucketName)
                uploadPartRequest.withKey(key)
                uploadPartRequest.withUploadId(uploadId)
                uploadPartRequest.withPartNumber(i)
                uploadPartRequest.setPartSize(c.size.toLong)
                uploadPartRequest.withInputStream(is)
                uploadPartRequest
              }
            } yield partReq.getPartETag
        }

      def completeUpload(uploadId: String): Pipe[F, List[PartETag], String] =
        _.evalMap { tags =>
          transferManager
            .getAmazonS3Client
            .completeMultipartUpload(
              new CompleteMultipartUploadRequest(awsConfig.bucketName, key, uploadId, tags.asJava)
            )
            .getETag
            .pure[F]
        }

      def cancelUpload(uploadId: String) =
        F.delay(
          transferManager
            .getAmazonS3Client
            .abortMultipartUpload(
              new AbortMultipartUploadRequest(awsConfig.bucketName, key, uploadId)
            )
        )

      in =>
        fs2
          .Stream
          .eval(initiateMultipartUpload)
          .flatMap { uploadId =>
            in.chunkMin(chunkSize)
              .zip(fs2.Stream.iterate(1)(_ + 1))
              .through(uploadPart(uploadId))
              .fold[List[PartETag]](List.empty)(_ :+ _)
              .through(completeUpload(uploadId))
              .handleErrorWith(ex =>
                fs2.Stream.eval(cancelUpload(uploadId) >> F.raiseError[String](ex))
              )
          }
    }

    /** <b color='green'>Download a file in a single request. Suitable for small files.</b>
      */

    override def downloadObject(key: String): Stream[F, Byte] =
      io.readInputStream(
        Sync[F].delay(
          transferManager.getAmazonS3Client.getObject(awsConfig.bucketName, key).getObjectContent
        ),
        chunkSize = 1024 * 1024,
      )

    /** <b color="green">Delete a file in a single request.</b>
      */

    override def deleteObject(key: String): Stream[F, Unit] =
      Stream.eval(
        F.delay(transferManager.getAmazonS3Client.deleteObject(awsConfig.bucketName, key))
      )

    override def listFiles: Stream[F, String] =
      Pagination.offsetUnfoldChunkEval[F, String, String] { maybeMarker =>
        val request = new ListObjectsRequest().withBucketName(awsConfig.bucketName)
        maybeMarker.foreach(request.setMarker)

        val res = transferManager.getAmazonS3Client.listObjects(request)
        val resultChunk =
          Chunk.seq(res.getObjectSummaries.asScala).map(_.getKey)
        val maybeNextMarker = Option(res.getNextMarker)

        F.delay((resultChunk, maybeNextMarker))
      }

    override def listBuckets: Stream[F, Bucket] =
      Stream.fromIterator(transferManager.getAmazonS3Client.listBuckets().asScala.iterator, 1024)

    override def objectUrl(key: String): F[URL] = {
      val presignedUrlRequest =
        new GeneratePresignedUrlRequest(awsConfig.bucketName, key)
          .withMethod(HttpMethod.GET)
          .withExpiration(expireTime())

      F.delay(transferManager.getAmazonS3Client.generatePresignedUrl(presignedUrlRequest))
    }
  }
}
