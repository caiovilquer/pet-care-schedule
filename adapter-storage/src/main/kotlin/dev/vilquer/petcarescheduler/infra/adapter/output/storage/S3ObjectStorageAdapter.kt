package dev.vilquer.petcarescheduler.infra.adapter.output.storage

import dev.vilquer.petcarescheduler.application.exception.UpstreamServiceException
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ObjectStoragePort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PresignedUpload
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

class S3ObjectStorageAdapter(private val properties: ObjectStorageProperties) : ObjectStoragePort, AutoCloseable {
    private val credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            properties.accessKey.requireConfigured("access-key"),
            properties.secretKey.requireConfigured("secret-key"),
        ),
    )
    private val endpoint = URI.create(properties.endpoint.requireConfigured("endpoint"))
    private val bucket = properties.bucket.requireConfigured("bucket")
    private val serviceConfiguration = S3Configuration.builder().pathStyleAccessEnabled(properties.pathStyle).build()
    private val httpClient = UrlConnectionHttpClient.builder()
        .connectionTimeout(properties.connectionTimeout)
        .socketTimeout(properties.socketTimeout)
        .build()
    private val client = S3Client.builder()
        .endpointOverride(endpoint)
        .region(Region.of(properties.region))
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfiguration)
        .httpClient(httpClient)
        .build()
    private val presigner = S3Presigner.builder()
        .endpointOverride(endpoint)
        .region(Region.of(properties.region))
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfiguration)
        .build()

    override fun presignUpload(key: String, contentType: String, checksumSha256: String, expiresIn: Duration): PresignedUpload = storageCall {
        val objectRequest = PutObjectRequest.builder()
            .bucket(bucket).key(key).contentType(contentType)
            .metadata(mapOf("sha256" to checksumSha256))
            .build()
        val signed = presigner.presignPutObject(
            PutObjectPresignRequest.builder().signatureDuration(expiresIn).putObjectRequest(objectRequest).build(),
        )
        val headers = signed.signedHeaders().entries
            .filterNot { it.key.equals("host", ignoreCase = true) }
            .associate { it.key to it.value.joinToString(",") }
        PresignedUpload(signed.url().toString(), headers)
    }

    override fun readObject(key: String, maxBytes: Long): ByteArray = storageCall {
        val head = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
        if (head.contentLength() > maxBytes) throw IllegalArgumentException("uploaded_file_too_large")
        client.getObject(
            GetObjectRequest.builder().bucket(bucket).key(key).build(),
            ResponseTransformer.toBytes(),
        ).asByteArray()
    }

    override fun promote(sourceKey: String, destinationKey: String, contentType: String) = storageCall {
        client.copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(sourceKey)
                .destinationBucket(bucket).destinationKey(destinationKey)
                .build(),
        )
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(sourceKey).build())
        Unit
    }

    override fun delete(key: String) = storageCall {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
        Unit
    }

    override fun presignDownload(key: String, expiresIn: Duration): String = storageCall {
        val request = GetObjectRequest.builder().bucket(bucket).key(key)
            .responseCacheControl("private, max-age=900")
            .build()
        presigner.presignGetObject(
            GetObjectPresignRequest.builder().signatureDuration(expiresIn).getObjectRequest(request).build(),
        ).url().toString()
    }

    private fun <T> storageCall(block: () -> T): T = try {
        block()
    } catch (ex: S3Exception) {
        throw UpstreamServiceException("Falha no armazenamento de mídia (${ex.statusCode()})")
    } catch (ex: SdkException) {
        throw UpstreamServiceException("Falha no armazenamento de mídia")
    }

    override fun close() {
        presigner.close(); client.close(); httpClient.close()
    }

    private fun String.requireConfigured(name: String): String =
        takeIf { it.isNotBlank() } ?: throw IllegalStateException("app.storage.$name não configurado")
}
