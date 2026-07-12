package dev.vilquer.petcarescheduler.infra.adapter.output.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class S3ObjectStorageAdapterTest {
    @Test
    fun `presigned upload binds content type and sha metadata without exposing credentials`() {
        val adapter = adapter()
        adapter.use {
            val signed = it.presignUpload(
                "staging/tutor/asset",
                "image/jpeg",
                "a".repeat(64),
                Duration.ofMinutes(3),
            )

            assertTrue(signed.url.startsWith("https://storage.example.test/rotinapet/staging/tutor/asset"))
            assertTrue(signed.url.contains("X-Amz-Expires=180"))
            assertEquals("image/jpeg", signed.headers.entries.first { header ->
                header.key.equals("content-type", ignoreCase = true)
            }.value)
            assertEquals("a".repeat(64), signed.headers.entries.first { header ->
                header.key.equals("x-amz-meta-sha256", ignoreCase = true)
            }.value)
            assertTrue(signed.headers.keys.none { header -> header.equals("host", ignoreCase = true) })
        }
    }

    @Test
    fun `presigned download is short lived and scoped to one object`() {
        adapter().use {
            val signed = it.presignDownload("media/tutor/avatar/asset.jpg", Duration.ofMinutes(15))
            assertTrue(signed.contains("/rotinapet/media/tutor/avatar/asset.jpg"))
            assertTrue(signed.contains("X-Amz-Expires=900"))
        }
    }

    private fun adapter() = S3ObjectStorageAdapter(
        ObjectStorageProperties(
            enabled = true,
            endpoint = "https://storage.example.test",
            region = "auto",
            bucket = "rotinapet",
            accessKey = "test-access-key",
            secretKey = "test-secret-key",
            pathStyle = true,
        ),
    )
}
