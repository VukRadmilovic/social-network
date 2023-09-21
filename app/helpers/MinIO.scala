package helpers

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import helpers.Implicits._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, GetObjectRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Configuration}

import java.io.File
import java.net.URI
import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}

object MinIO {
  private val config = ConfigFactory.load()

  private val bucketName = "profile-pictures"
  private val region = Region.US_EAST_1
  private val endpoint = new URI(config.getString("awsEndpoint"))
  private val credentials = StaticCredentialsProvider.create(
    AwsBasicCredentials.create(sys.env("AwsAccessKeyId"), sys.env("AwsSecretAccessKey")))
  private val serviceConfiguration = S3Configuration.builder().pathStyleAccessEnabled(true).build

  private implicit val minIOExecutionContext: ExecutionContext = ActorSystem().dispatchers.lookup("minio-context")

  private def getS3AsyncClient: S3AsyncClient = {
    S3AsyncClient.builder()
      .region(region)
      .credentialsProvider(credentials)
      .serviceConfiguration(serviceConfiguration)
      .endpointOverride(endpoint)
      .build()
  }

  private def getS3Presigner: S3Presigner = {
    S3Presigner.builder()
      .region(region)
      .credentialsProvider(credentials)
      .serviceConfiguration(serviceConfiguration)
      .endpointOverride(endpoint)
      .build()
  }

  def uploadProfilePicture(username: String, file: File, contentType: String): Future[Unit] = {
    val s3AsyncClient = getS3AsyncClient

    val request = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(username)
      .contentType(contentType)
      .build()

    val asyncRequestBody = AsyncRequestBody.fromFile(file.toPath)

    val uploadFuture = s3AsyncClient.putObject(request, asyncRequestBody)

    uploadFuture.toScala.map(_ => ())
  }

  def deleteProfilePicture(username: String): Future[Unit] = {
    val s3AsyncClient = getS3AsyncClient

    val request = DeleteObjectRequest.builder()
      .bucket(bucketName)
      .key(username)
      .build()

    s3AsyncClient.deleteObject(request).toScala.map(_ => ())
  }

  def getProfilePicture(username: String): Future[String] = {
    val presigner = getS3Presigner

    val getObjectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(username)
      .build()

    val getObjectPresignRequest = GetObjectPresignRequest.builder
      .signatureDuration(Duration.ofDays(7))
      .getObjectRequest(getObjectRequest)
      .build()

    Future {
      presigner.presignGetObject(getObjectPresignRequest).url.toString
    }
  }
}
