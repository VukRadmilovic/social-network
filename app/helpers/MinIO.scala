package helpers

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, GetObjectRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.{File, FileInputStream}
import java.net.URI
import java.time.Duration

object MinIO {
  private def getCredentials: StaticCredentialsProvider = {
    StaticCredentialsProvider.create(AwsBasicCredentials.create("RTKIDBuVGP2PZW0rrVJf",
      "2YcFm9NBgzi3ZDjQg5OLqjyiuD78YYPxAutHlNlc"))
  }

  private def getServiceConfiguration: S3Configuration = {
    S3Configuration.builder().pathStyleAccessEnabled(true).build
  }

  private def getEndpoint = {
    new URI("http://localhost:9000")
  }

  private def getS3Client: S3Client = {
    S3Client.builder()
      .region(Region.US_EAST_1)
      .credentialsProvider(getCredentials)
      .serviceConfiguration(getServiceConfiguration)
      .endpointOverride(getEndpoint)
      .build
  }

  private def getS3Presigner: S3Presigner = {
    S3Presigner.builder()
      .region(Region.US_EAST_1)
      .credentialsProvider(getCredentials)
      .serviceConfiguration(getServiceConfiguration)
      .endpointOverride(getEndpoint)
      .build()
  }

  def uploadProfilePicture(username: String, file: File, contentType: String): Unit = {
    val bucketName = "profile-pictures"
    val s3Client = getS3Client
    val inputStream = new FileInputStream(file)

    s3Client.putObject(PutObjectRequest
      .builder()
      .bucket(bucketName)
      .key(username)
      .contentType(contentType)
      .build(),
      RequestBody.fromFile(file))

    inputStream.close()
  }

  def deleteProfilePicture(username: String): Unit = {
    val bucketName = "profile-pictures"
    val s3Client = getS3Client

    val deleteObjectRequest: DeleteObjectRequest = DeleteObjectRequest.builder()
      .bucket(bucketName)
      .key(username)
      .build()

    s3Client.deleteObject(deleteObjectRequest)
  }

  def getProfilePicture(username: String): String = {
    val bucketName = "profile-pictures"
    val presigner = getS3Presigner

    val getObjectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(username)
      .build()

    val getObjectPresignRequest = GetObjectPresignRequest.builder.signatureDuration(Duration.ofDays(7)).getObjectRequest(getObjectRequest).build
    presigner.presignGetObject(getObjectPresignRequest).url.toString
  }
}
