package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.usecase.command.NearbySearchQuery
import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.GeocodingPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PlacesCachePort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PlacesPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlaceDetailsUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlacePhotoUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetPlaceReviewsUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SearchLocationsUseCase
import dev.vilquer.petcarescheduler.usecase.result.DaySchedule
import dev.vilquer.petcarescheduler.usecase.result.LocationItem
import dev.vilquer.petcarescheduler.usecase.result.LocationSearchResult
import dev.vilquer.petcarescheduler.usecase.result.OpeningHoursWeek
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import dev.vilquer.petcarescheduler.usecase.result.PlaceSummary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orquestra a busca de petshops/veterinários e o acesso a detalhes/reviews/fotos,
 * sempre passando pelo [PlacesCachePort] (cache compartilhado entre usuários —
 * a principal economia desta migração: 1 busca paga, todas as seguintes com o
 * mesmo CEP/place reaproveitam o resultado).
 *
 * Distância é calculada aqui via Haversine (grátis) em vez de Distance Matrix
 * (faturado por elemento); a grade semanal de horários só é buscada sob
 * demanda em [getDetails], não durante a busca — do contrário seria 1 Place
 * Details extra por resultado a cada busca.
 */
class LocationAppService(
    private val geocoding: GeocodingPort,
    private val places: PlacesPort,
    private val cache: PlacesCachePort
) : SearchLocationsUseCase, GetPlaceDetailsUseCase, GetPlaceReviewsUseCase, GetPlacePhotoUseCase {

    override fun search(query: NearbySearchQuery): LocationSearchResult {
        val geo = cache.getOrCompute(geocodeKey(query.zipCode), GEOCODE_TTL_SECONDS) {
            geocoding.geocode(query.zipCode)
        } ?: throw NotFoundException("CEP não encontrado: ${query.zipCode}")

        val radiusMeters = (query.radiusKm * 1000).roundToInt()
        // Amplia 20% o raio de busca (limitado a 50km) para não perder locais
        // antes do filtro fino por distância real feito abaixo.
        val searchRadiusMeters = min((radiusMeters * 1.2).roundToInt(), 50_000)

        val summaries = cache.getOrCompute(
            nearbyKey(query.category, geo.latitude, geo.longitude, radiusMeters),
            NEARBY_TTL_SECONDS
        ) { places.searchNearby(geo.latitude, geo.longitude, searchRadiusMeters, query.category) }

        var withDistance = summaries.map { summary ->
            summary to haversineKm(geo.latitude, geo.longitude, summary.latitude, summary.longitude)
        }.filter { (_, distanceKm) -> distanceKm <= query.radiusKm }

        if (query.isOpenNow) {
            withDistance = withDistance.filter { (summary, _) -> summary.openNow == true }
        }

        val deduped = LinkedHashMap<String, Pair<PlaceSummary, Double>>()
        withDistance.forEach { pair -> deduped.putIfAbsent(dedupeKey(pair.first), pair) }

        val sorted = sortResults(deduped.values.toList(), query.sortBy)
        val items = sorted.take(MAX_RESULTS).map { (summary, distanceKm) ->
            toLocationItem(summary, distanceKm, query.category)
        }

        return LocationSearchResult(items, items.size)
    }

    override fun getDetails(placeId: String): PlaceDetails =
        cache.getOrCompute(detailsKey(placeId), DETAILS_TTL_SECONDS) { places.details(placeId) }
            ?: throw NotFoundException("Local não encontrado: $placeId")

    override fun getReviews(placeId: String): List<PlaceReview> =
        cache.getOrCompute(reviewsKey(placeId), REVIEWS_TTL_SECONDS) { places.reviews(placeId) }

    override fun getPhoto(photoReference: String, maxWidth: Int): PlacePhoto =
        cache.getOrCompute(photoKey(photoReference, maxWidth), PHOTO_TTL_SECONDS) {
            places.fetchPhoto(photoReference, maxWidth)
        }

    // ===== chaves de cache =====

    private fun geocodeKey(zipCode: String) = "geocode:${zipCode.filter { it.isDigit() }}"

    private fun nearbyKey(category: PlaceCategory, lat: Double, lng: Double, radiusMeters: Int): String {
        // Arredonda coordenadas/raio para agrupar buscas praticamente idênticas
        // no mesmo slot de cache (mesmo racional do front antes da migração).
        val latRounded = (lat * 1000).roundToInt() / 1000.0
        val lngRounded = (lng * 1000).roundToInt() / 1000.0
        val radiusRounded = radiusMeters / 1000
        return "nearby:$category:$latRounded:$lngRounded:$radiusRounded"
    }

    private fun detailsKey(placeId: String) = "details:$placeId"
    private fun reviewsKey(placeId: String) = "reviews:$placeId"
    private fun photoKey(photoReference: String, maxWidth: Int) = "photo:$photoReference:$maxWidth"

    // ===== dedup / sort =====

    private fun dedupeKey(summary: PlaceSummary): String =
        summary.placeId.ifBlank { "${normalizeName(summary.name)}|${summary.address}" }

    private fun normalizeName(name: String): String = name
        .lowercase()
        .replace(Regex("[áàãâ]"), "a")
        .replace(Regex("[éêë]"), "e")
        .replace(Regex("[íîï]"), "i")
        .replace(Regex("[óôõö]"), "o")
        .replace(Regex("[úûü]"), "u")
        .replace(Regex("[ç]"), "c")
        .replace(Regex("[^a-z0-9\\s]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun sortResults(
        items: List<Pair<PlaceSummary, Double>>,
        sortBy: String
    ): List<Pair<PlaceSummary, Double>> = when (sortBy) {
        "rating" -> items.sortedByDescending { it.first.rating ?: 0.0 }
        "name" -> items.sortedBy { it.first.name }
        else -> items.sortedBy { it.second }
    }

    // ===== conversão para o contrato exposto ao frontend =====

    private fun toLocationItem(summary: PlaceSummary, distanceKm: Double, category: PlaceCategory): LocationItem {
        val addressParts = summary.address.split(",").map { it.trim() }
        val neighborhood = addressParts.getOrNull(1) ?: ""
        val city = addressParts.getOrNull(2) ?: ""
        val state = Regex("([A-Z]{2})").find(addressParts.lastOrNull() ?: "")?.groupValues?.get(1) ?: ""

        val hasGrooming = inferFromTypes(summary.types, GROOMING_TYPES)
        val hasDaycare = inferFromTypes(summary.types, DAYCARE_TYPES)
        val hasHotel = inferFromTypes(summary.types, HOTEL_TYPES)
        val hasVaccination = inferFromTypes(summary.types, VACCINATION_TYPES)
        val hasEmergency = EMERGENCY_KEYWORDS.any { summary.name.lowercase().contains(it) }

        val isPetshop = category == PlaceCategory.PETSHOP
        val services = mutableListOf(if (isPetshop) "petshop" else "veterinary")
        if (isPetshop) {
            if (hasGrooming) services.add("grooming")
            if (hasDaycare) services.add("daycare")
            if (hasHotel) services.add("hotel")
            if (hasVaccination) services.add("vaccination")
        } else if (hasEmergency) {
            services.add("emergency")
        }

        return LocationItem(
            id = summary.placeId,
            name = summary.name,
            address = summary.address,
            neighborhood = neighborhood,
            city = city,
            state = state,
            latitude = summary.latitude,
            longitude = summary.longitude,
            phone = null,
            rating = summary.rating ?: 0.0,
            reviewCount = summary.userRatingsTotal ?: 0,
            distance = distanceKm,
            distanceText = "%.1f km".format(distanceKm),
            isOpen = summary.openNow ?: false,
            openingHours = EMPTY_WEEK,
            services = services,
            type = if (isPetshop) "petshop" else "veterinary",
            photos = summary.photoReference?.let { listOf(it) } ?: emptyList(),
            website = null,
            hasGrooming = if (isPetshop) hasGrooming else null,
            hasDaycare = if (isPetshop) hasDaycare else null,
            hasHotel = if (isPetshop) hasHotel else null,
            hasVaccination = if (isPetshop) hasVaccination else null,
            hasEmergency = if (isPetshop) null else hasEmergency,
            hasLaboratory = if (isPetshop) null else false,
            hasSurgery = if (isPetshop) null else false,
            hasRadiology = if (isPetshop) null else false,
            specialties = emptyList()
        )
    }

    private fun inferFromTypes(types: List<String>, keywords: List<String>): Boolean =
        types.any { type -> keywords.any { keyword -> type.contains(keyword, ignoreCase = true) } }

    companion object {
        private const val MAX_RESULTS = 5
        private const val GEOCODE_TTL_SECONDS = 24 * 60 * 60L
        private const val NEARBY_TTL_SECONDS = 2 * 60 * 60L
        private const val DETAILS_TTL_SECONDS = 12 * 60 * 60L
        private const val REVIEWS_TTL_SECONDS = 6 * 60 * 60L
        private const val PHOTO_TTL_SECONDS = 7 * 24 * 60 * 60L

        private val GROOMING_TYPES = listOf("pet_groomer", "pet grooming")
        private val DAYCARE_TYPES = listOf("pet_store", "pet boarding")
        private val HOTEL_TYPES = listOf("pet boarding", "pet hotel")
        private val VACCINATION_TYPES = listOf("veterinary_care", "pet_store")
        private val EMERGENCY_KEYWORDS =
            listOf("24h", "24 horas", "emergencia", "emergência", "urgencia", "urgência", "hospital")

        private val CLOSED_DAY = DaySchedule(isOpen = false)
        private val EMPTY_WEEK =
            OpeningHoursWeek(CLOSED_DAY, CLOSED_DAY, CLOSED_DAY, CLOSED_DAY, CLOSED_DAY, CLOSED_DAY, CLOSED_DAY)

        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val earthRadiusKm = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusKm * c
        }
    }
}
