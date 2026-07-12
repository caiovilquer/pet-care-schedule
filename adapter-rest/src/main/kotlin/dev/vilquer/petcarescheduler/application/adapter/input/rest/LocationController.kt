package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.LocationDtoMapper
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlaceDetailsUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlacePhotoUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlaceReviewsUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SearchLocationsUseCase
import dev.vilquer.petcarescheduler.usecase.result.LocationSearchResult
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Duration

/**
 * Endpoints autenticados (padrão de `SecurityConfig`, nada adicionado a
 * permitAll): só um usuário logado dispara custo de API do Google.
 */
@RestController
@RequestMapping("/api/v1/locations")
@Validated
class LocationController(
    private val mapper: LocationDtoMapper,
    private val searchLocations: SearchLocationsUseCase,
    private val getPlaceDetails: GetPlaceDetailsUseCase,
    private val getPlaceReviews: GetPlaceReviewsUseCase,
    private val getPlacePhoto: GetPlacePhotoUseCase
) {

    @GetMapping("/petshops")
    fun petshops(
        @RequestParam zipCode: String,
        @RequestParam(defaultValue = "5") radius: Double,
        @RequestParam(defaultValue = "false") isOpenNow: Boolean,
        @RequestParam(defaultValue = "distance") sortBy: String
    ): LocationSearchResult =
        searchLocations.search(mapper.toPetshopQuery(zipCode, radius, isOpenNow, sortBy))

    @GetMapping("/veterinaries")
    fun veterinaries(
        @RequestParam zipCode: String,
        @RequestParam(defaultValue = "5") radius: Double,
        @RequestParam(defaultValue = "false") isOpenNow: Boolean,
        @RequestParam(defaultValue = "distance") sortBy: String
    ): LocationSearchResult =
        searchLocations.search(mapper.toVeterinaryQuery(zipCode, radius, isOpenNow, sortBy))

    @GetMapping("/{placeId}/details")
    fun details(@PathVariable placeId: String): PlaceDetails = getPlaceDetails.getDetails(placeId)

    @GetMapping("/{placeId}/reviews")
    fun reviews(@PathVariable placeId: String): List<PlaceReview> = getPlaceReviews.getReviews(placeId)

    // Proxy: os bytes da foto passam pelo backend para que a chave de API do
    // Google nunca precise ser exposta ao navegador para esse fim.
    @GetMapping("/photo")
    fun photo(
        @RequestParam ref: String,
        @RequestParam(defaultValue = "800") @Min(100) @Max(1600) maxWidth: Int
    ): ResponseEntity<ByteArray> {
        val photo = getPlacePhoto.getPhoto(ref, maxWidth)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
            .body(photo.bytes)
    }
}
