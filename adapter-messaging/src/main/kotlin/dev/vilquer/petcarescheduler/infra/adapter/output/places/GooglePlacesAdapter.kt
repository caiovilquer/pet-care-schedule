package dev.vilquer.petcarescheduler.infra.adapter.output.places

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.vilquer.petcarescheduler.application.exception.UpstreamServiceException
import dev.vilquer.petcarescheduler.infra.config.GoogleApiProps
import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PlacesPort
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import dev.vilquer.petcarescheduler.usecase.result.PlaceSummary
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GooglePlacesAdapter(
    @param:Qualifier("googlePlacesClient") private val http: RestClient,
    private val props: GoogleApiProps
) : PlacesPort {

    private val log = LoggerFactory.getLogger(GooglePlacesAdapter::class.java)
    private val mapper = ObjectMapper()

    override fun searchNearby(latitude: Double, longitude: Double, radiusMeters: Int, category: PlaceCategory): List<PlaceSummary> {
        val (type, keyword) = when (category) {
            PlaceCategory.PETSHOP -> "pet_store" to "petshop pet shop"
            PlaceCategory.VETERINARY -> "veterinary_care" to "veterinario clinica veterinaria"
        }

        val json = http.get().uri { builder ->
            builder.path("/place/nearbysearch/json")
                .queryParam("location", "$latitude,$longitude")
                .queryParam("radius", radiusMeters)
                .queryParam("type", type)
                .queryParam("keyword", keyword)
                .queryParam("language", props.language)
                .queryParam("key", props.apiKey)
                .build()
        }.retrieve().body(String::class.java) ?: return emptyList()

        val root = mapper.readTree(json)
        val status = root.path("status").asText()
        if (status != "OK" && status != "ZERO_RESULTS") {
            log.error("Google Places nearbysearch retornou status {}", status)
            throw UpstreamServiceException("Google Places nearbysearch retornou status $status")
        }

        return root.path("results").map { it.toPlaceSummary() }
    }

    override fun details(placeId: String): PlaceDetails? {
        val fields = listOf(
            "place_id", "formatted_phone_number", "international_phone_number",
            "website", "url", "photos", "price_level", "editorial_summary",
            "opening_hours", "types", "vicinity", "business_status"
        ).joinToString(",")

        val json = http.get().uri { builder ->
            builder.path("/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", fields)
                .queryParam("language", props.language)
                .queryParam("key", props.apiKey)
                .build()
        }.retrieve().body(String::class.java) ?: return null

        val root = mapper.readTree(json)
        val status = root.path("status").asText()
        if (status == "NOT_FOUND" || status == "INVALID_REQUEST") return null
        if (status != "OK") {
            log.error("Google Places details retornou status {} para {}", status, placeId)
            throw UpstreamServiceException("Google Places details retornou status $status")
        }

        val result = root.path("result")
        val weekdayText = result.path("opening_hours").path("weekday_text").map { it.asText() }

        return PlaceDetails(
            placeId = result.textOrNull("place_id") ?: placeId,
            description = result.path("editorial_summary").textOrNull("overview") ?: "",
            phone = result.textOrNull("formatted_phone_number") ?: result.textOrNull("international_phone_number"),
            website = result.textOrNull("website"),
            googleMapsUrl = result.textOrNull("url"),
            types = result.path("types").map { it.asText() },
            vicinity = result.textOrNull("vicinity") ?: "",
            priceLevel = result.intOrNull("price_level"),
            businessStatus = result.textOrNull("business_status"),
            openingHours = if (weekdayText.isEmpty()) null else GoogleOpeningHoursParser.parse(weekdayText),
            photoReferences = result.path("photos").take(5).map { it.path("photo_reference").asText() }
        )
    }

    override fun reviews(placeId: String): List<PlaceReview> {
        val json = http.get().uri { builder ->
            builder.path("/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "reviews")
                .queryParam("language", props.language)
                .queryParam("key", props.apiKey)
                .build()
        }.retrieve().body(String::class.java) ?: return emptyList()

        val root = mapper.readTree(json)
        if (root.path("status").asText() != "OK") return emptyList()

        return root.path("result").path("reviews").take(5).map { r ->
            PlaceReview(
                authorName = r.textOrNull("author_name") ?: "",
                rating = r.intOrNull("rating") ?: 0,
                text = r.textOrNull("text") ?: "",
                relativeTime = r.textOrNull("relative_time_description") ?: "",
                profilePhotoUrl = r.textOrNull("profile_photo_url")
            )
        }
    }

    override fun fetchPhoto(photoReference: String, maxWidth: Int): PlacePhoto {
        val response = http.get().uri { builder ->
            builder.path("/place/photo")
                .queryParam("maxwidth", maxWidth.coerceIn(100, 1600))
                .queryParam("photo_reference", photoReference)
                .queryParam("key", props.apiKey)
                .build()
        }.retrieve().toEntity(ByteArray::class.java)

        val contentType = response.headers.contentType?.toString() ?: "image/jpeg"
        return PlacePhoto(response.body ?: ByteArray(0), contentType)
    }

    private fun JsonNode.toPlaceSummary(): PlaceSummary {
        val location = path("geometry").path("location")
        val photoRef = path("photos").firstOrNull()?.path("photo_reference")?.asText()

        return PlaceSummary(
            placeId = textOrNull("place_id") ?: "",
            name = textOrNull("name") ?: "",
            address = textOrNull("formatted_address") ?: textOrNull("vicinity") ?: "",
            latitude = location.path("lat").asDouble(),
            longitude = location.path("lng").asDouble(),
            rating = doubleOrNull("rating"),
            userRatingsTotal = intOrNull("user_ratings_total"),
            businessStatus = textOrNull("business_status"),
            openNow = path("opening_hours").boolOrNull("open_now"),
            photoReference = photoRef,
            types = path("types").map { it.asText() },
            priceLevel = intOrNull("price_level")
        )
    }

    private fun JsonNode.textOrNull(field: String): String? =
        path(field).let { if (it.isMissingNode || it.isNull) null else it.asText() }

    private fun JsonNode.intOrNull(field: String): Int? =
        path(field).let { if (it.isNumber) it.asInt() else null }

    private fun JsonNode.doubleOrNull(field: String): Double? =
        path(field).let { if (it.isNumber) it.asDouble() else null }

    private fun JsonNode.boolOrNull(field: String): Boolean? =
        path(field).let { if (it.isBoolean) it.asBoolean() else null }
}
