package helpers

import _root_.org.apache.commons.io.IOUtils
import io.minio._

import java.io.{File, FileInputStream}
import java.util.Base64


object MinIO extends App {
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

    val stream = minioClient.getObject(GetObjectArgs.builder.bucket(bucketName).`object`(username).build)
    Base64.getEncoder.encodeToString(IOUtils.toByteArray(stream))
  }
}
