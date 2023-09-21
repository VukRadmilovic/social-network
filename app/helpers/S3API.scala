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

object S3API {
  private val config = ConfigFactory.load()

  private val region = Region.US_EAST_1
  private val endpoint = new URI(config.getString("awsEndpoint"))
  private val credentials = StaticCredentialsProvider.create(
    AwsBasicCredentials.create(sys.env("AwsAccessKeyId"), sys.env("AwsSecretAccessKey")))
  private val serviceConfiguration = S3Configuration.builder().pathStyleAccessEnabled(true).build

  private implicit val s3ExecutionContext: ExecutionContext = ActorSystem().dispatchers.lookup("s3-context")

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

  def put(bucket: String, key: String, file: File, contentType: String): Future[Unit] = {
    val s3AsyncClient = getS3AsyncClient

    val request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .contentType(contentType)
      .build()

    val asyncRequestBody = AsyncRequestBody.fromFile(file.toPath)
    val putFuture = s3AsyncClient.putObject(request, asyncRequestBody)
    putFuture.toScala.map(_ => ())
  }

  def delete(bucket: String, key: String): Future[Unit] = {
    val s3AsyncClient = getS3AsyncClient

    val request = DeleteObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()

    s3AsyncClient.deleteObject(request).toScala.map(_ => ())
  }

  def get(bucket: String, key: String): Future[String] = {
    val presigner = getS3Presigner

    val getObjectRequest = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
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
