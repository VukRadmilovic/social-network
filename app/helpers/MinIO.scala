package helpers

import io.minio._
import io.minio.http.Method

import java.io.{File, FileInputStream}

object MinIO {
  def uploadProfilePicture(username: String, file: File, contentType: String): Unit = {
    val bucketName = "profile-pictures"
    val minioClient = MinioClient
      .builder
      .endpoint("http://localhost:9000")
      .credentials("admin", "password")
      .build
    val inputStream = new FileInputStream(file)

    minioClient.putObject(PutObjectArgs.builder()
      .bucket(bucketName)
      .`object`(username)
      .stream(inputStream, inputStream.available(), -1)
      .contentType(contentType)
      .build())

    inputStream.close()
  }

  def deleteProfilePicture(username: String): Unit = {
    val bucketName = "profile-pictures"
    val minioClient = MinioClient
      .builder
      .endpoint("http://localhost:9000")
      .credentials("admin", "password")
      .build

    minioClient.removeObject(RemoveObjectArgs.builder.bucket(bucketName).`object`(username).build)
  }

  def getProfilePicture(username: String): String = {
    val bucketName = "profile-pictures"
    val minioClient = MinioClient
      .builder
      .endpoint("http://localhost:9000")
      .credentials("admin", "password")
      .build

    val args = GetPresignedObjectUrlArgs.builder.method(Method.GET).bucket(bucketName).`object`(username).build
    minioClient.getPresignedObjectUrl(args)
  }
}
