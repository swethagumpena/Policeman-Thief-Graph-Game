package utils

import java.io._
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, S3Exception}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import java.time.Duration

object WriteResultToFile {
  def gameSummary(filePath: String, content: List[String]): Unit = {
    if (filePath.startsWith("s3://")) {
      writeToS3(filePath, content)
    } else {
      writeToLocal(filePath, content)
    }
  }

  private def writeToS3(filePath: String, content: List[String]): Unit = {
    // Use the Apache HTTP client explicitly
    val apacheHttpClient = ApacheHttpClient.builder().build()

    val s3Client = S3Client.builder()
      .region(Region.US_EAST_1)
      .httpClient(apacheHttpClient) // Provide the custom HTTP client
      .overrideConfiguration(
        ClientOverrideConfiguration.builder()
          .apiCallTimeout(Duration.ofSeconds(10))
          .build()
      )
      .build()

    try {
      val concatenatedContent = content.mkString("\n") // Concatenate strings with newline separator
      val inputStream = new java.io.ByteArrayInputStream(concatenatedContent.getBytes("UTF-8"))
      val requestBody = RequestBody.fromInputStream(inputStream, concatenatedContent.length())

      val bucketName = getBucketName(filePath)
      val key = getKey(filePath)

      val request = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType("text/plain")
        .build()

      s3Client.putObject(request, requestBody)
    } catch {
      case e: S3Exception => e.printStackTrace()
    } finally {
      s3Client.close()
    }
  }

  private def writeToLocal(filePath: String, content: List[String]): Unit = {
    val writer = new PrintWriter(filePath)
    try {
      content.foreach(writer.println)
    } finally {
      writer.close()
    }
  }

  def getBucketName(s3Path: String): String = {
    s3Path.drop(5).takeWhile(_ != '/')
  }

  def getKey(s3Path: String): String = {
    s3Path.drop(5 + getBucketName(s3Path).length + 1)
  }
}
