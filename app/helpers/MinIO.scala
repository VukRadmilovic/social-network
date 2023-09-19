package helpers

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.io.{File, FileInputStream}
import java.net.URI

object MinIO {
  def uploadProfilePicture(username: String, file: File, contentType: String): Unit = {
    val bucketName = "profile-pictures"
     val s3Client = S3Client.builder()
       .region(Region.US_EAST_1)
       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("RTKIDBuVGP2PZW0rrVJf", "2YcFm9NBgzi3ZDjQg5OLqjyiuD78YYPxAutHlNlc")))
       .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build)
       .endpointOverride(new URI("http://localhost:9000"))
       .build

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
    val s3Client = S3Client.builder()
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("RTKIDBuVGP2PZW0rrVJf", "2YcFm9NBgzi3ZDjQg5OLqjyiuD78YYPxAutHlNlc")))
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build)
      .endpointOverride(new URI("http://localhost:9000"))
      .build

//    s3Client.removeObject(RemoveObjectArgs.builder.bucket(bucketName).`object`(username).build)
  }

  def getProfilePicture(username: String): String = {
    val bucketName = "profile-pictures"
    val s3Client = S3Client.builder()
      .region(Region.US_EAST_1)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("RTKIDBuVGP2PZW0rrVJf", "2YcFm9NBgzi3ZDjQg5OLqjyiuD78YYPxAutHlNlc")))
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build)
      .endpointOverride(new URI("http://localhost:9000"))
      .build
    "s"
//    val args = GetPresignedObjectUrlArgs.builder.method(Method.GET).bucket(bucketName).`object`(username).build
//    minioClient.getPresignedObjectUrl(args)
  }
}
