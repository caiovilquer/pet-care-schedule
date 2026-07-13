package dev.vilquer.petcarescheduler.infra.adapter.output.storage

import dev.vilquer.petcarescheduler.application.exception.UpstreamServiceException
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ObjectStoragePort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PresignedUpload
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties::class)
class ObjectStorageConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = ["enabled"], havingValue = "true")
    fun s3ObjectStorage(properties: ObjectStorageProperties): ObjectStoragePort = S3ObjectStorageAdapter(properties)

    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    fun unavailableObjectStorage(): ObjectStoragePort = object : ObjectStoragePort {
        private fun unavailable(): Nothing = throw UpstreamServiceException("Armazenamento de mídia não configurado")
        override fun presignUpload(key: String, contentType: String, checksumSha256: String, expiresIn: Duration): PresignedUpload = unavailable()
        override fun readObject(key: String, maxBytes: Long): ByteArray = unavailable()
        override fun promote(sourceKey: String, destinationKey: String, contentType: String) = unavailable()
        override fun delete(key: String) = unavailable()
        override fun presignDownload(key: String, expiresIn: Duration, downloadFilename: String?): String = unavailable()
    }
}
