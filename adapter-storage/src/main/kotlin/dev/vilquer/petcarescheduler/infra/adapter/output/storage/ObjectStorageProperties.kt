package dev.vilquer.petcarescheduler.infra.adapter.output.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.storage")
data class ObjectStorageProperties(
    var enabled: Boolean = false,
    var endpoint: String = "",
    var region: String = "us-east-1",
    var bucket: String = "",
    var accessKey: String = "",
    var secretKey: String = "",
    var pathStyle: Boolean = false,
    var connectionTimeout: Duration = Duration.ofSeconds(5),
    var socketTimeout: Duration = Duration.ofSeconds(20),
)
