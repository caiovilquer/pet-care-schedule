package dev.vilquer.petcarescheduler.infra.adapter.output.places

import com.fasterxml.jackson.databind.ObjectMapper
import dev.vilquer.petcarescheduler.application.exception.UpstreamServiceException
import dev.vilquer.petcarescheduler.infra.config.GoogleApiProps
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.GeocodingPort
import dev.vilquer.petcarescheduler.usecase.result.GeoLocation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GoogleGeocodingAdapter(
    @param:Qualifier("googlePlacesClient") private val http: RestClient,
    private val props: GoogleApiProps
) : GeocodingPort {

    private val log = LoggerFactory.getLogger(GoogleGeocodingAdapter::class.java)
    private val mapper = ObjectMapper()

    override fun geocode(zipCode: String): GeoLocation? {
        val json = http.get().uri { builder ->
            builder.path("/geocode/json")
                .queryParam("address", "$zipCode, Brazil")
                .queryParam("region", props.region)
                .queryParam("language", props.language)
                .queryParam("key", props.apiKey)
                .build()
        }.retrieve().body(String::class.java) ?: return null

        val root = mapper.readTree(json)
        val status = root.path("status").asText()
        if (status == "ZERO_RESULTS" || status == "INVALID_REQUEST") return null
        if (status != "OK") {
            log.error("Google Geocoding retornou status {} para CEP {}", status, zipCode)
            throw UpstreamServiceException("Google Geocoding retornou status $status")
        }

        val result = root.path("results").firstOrNull() ?: return null
        val location = result.path("geometry").path("location")
        val components = result.path("address_components")

        fun component(type: String): String =
            components.firstOrNull { c -> c.path("types").any { it.asText() == type } }
                ?.path("long_name")?.asText() ?: ""

        return GeoLocation(
            latitude = location.path("lat").asDouble(),
            longitude = location.path("lng").asDouble(),
            formattedAddress = result.path("formatted_address").asText(""),
            city = component("administrative_area_level_2"),
            state = component("administrative_area_level_1"),
            country = component("country"),
            zipCode = component("postal_code").ifBlank { zipCode }
        )
    }
}
