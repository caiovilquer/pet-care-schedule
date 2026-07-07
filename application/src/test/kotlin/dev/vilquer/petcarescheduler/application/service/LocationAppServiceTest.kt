package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeGeocodingPort
import dev.vilquer.petcarescheduler.application.FakePlacesCachePort
import dev.vilquer.petcarescheduler.application.FakePlacesPort
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.usecase.command.NearbySearchQuery
import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import dev.vilquer.petcarescheduler.usecase.result.GeoLocation
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import dev.vilquer.petcarescheduler.usecase.result.PlaceSummary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocationAppServiceTest {

    private val saoPaulo = GeoLocation(
        latitude = -23.5505,
        longitude = -46.6333,
        formattedAddress = "Av. Paulista, São Paulo - SP",
        city = "São Paulo",
        state = "São Paulo",
        country = "Brasil",
        zipCode = "01310-100"
    )

    private fun place(
        placeId: String = "place-1",
        name: String = "Petshop Central",
        lat: Double = -23.5505,
        lng: Double = -46.6333,
        rating: Double? = 4.5,
        openNow: Boolean? = true,
        types: List<String> = listOf("pet_store")
    ) = PlaceSummary(
        placeId = placeId,
        name = name,
        address = "Rua A, Bairro B, Cidade C - SP",
        latitude = lat,
        longitude = lng,
        rating = rating,
        userRatingsTotal = 10,
        businessStatus = "OPERATIONAL",
        openNow = openNow,
        photoReference = "photo-ref",
        types = types,
        priceLevel = 1
    )

    private fun query(
        radiusKm: Double = 5.0,
        category: PlaceCategory = PlaceCategory.PETSHOP,
        isOpenNow: Boolean = false,
        sortBy: String = "distance"
    ) = NearbySearchQuery("01310-100", radiusKm, category, isOpenNow, sortBy)

    @Test
    fun `search throws NotFoundException when geocode fails`() {
        val service = LocationAppService(FakeGeocodingPort(null), FakePlacesPort(), FakePlacesCachePort())
        assertThrows(NotFoundException::class.java) { service.search(query()) }
    }

    @Test
    fun `search filters out places beyond the requested radius`() {
        // ~15km de distância em latitude a partir de São Paulo
        val far = place(placeId = "far", lat = -23.6900, lng = -46.6333)
        val places = FakePlacesPort(nearby = listOf(place(placeId = "near"), far))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query(radiusKm = 5.0))

        assertEquals(1, result.total)
        assertEquals("near", result.locations[0].id)
    }

    @Test
    fun `search filters by isOpenNow when requested`() {
        val closed = place(placeId = "closed", openNow = false)
        val open = place(placeId = "open", openNow = true)
        val places = FakePlacesPort(nearby = listOf(closed, open))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query(isOpenNow = true))

        assertEquals(1, result.total)
        assertEquals("open", result.locations[0].id)
    }

    @Test
    fun `search dedupes places sharing the same placeId`() {
        val places = FakePlacesPort(nearby = listOf(place(placeId = "dup"), place(placeId = "dup")))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query())

        assertEquals(1, result.total)
    }

    @Test
    fun `search caps results at 5`() {
        val many = (1..10).map { place(placeId = "p$it") }
        val places = FakePlacesPort(nearby = many)
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query())

        assertEquals(5, result.total)
    }

    @Test
    fun `search sorts by rating descending when requested`() {
        val low = place(placeId = "low", rating = 3.0)
        val high = place(placeId = "high", rating = 5.0)
        val places = FakePlacesPort(nearby = listOf(low, high))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query(sortBy = "rating"))

        assertEquals("high", result.locations[0].id)
        assertEquals("low", result.locations[1].id)
    }

    @Test
    fun `search infers grooming service for petshops from Google types`() {
        val groomer = place(types = listOf("pet_store", "pet_groomer"))
        val places = FakePlacesPort(nearby = listOf(groomer))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query(category = PlaceCategory.PETSHOP))

        val item = result.locations[0]
        assertEquals(true, item.hasGrooming)
        assertTrue(item.services.contains("grooming"))
        assertNull(item.hasEmergency)
    }

    @Test
    fun `search infers veterinary emergency from name keyword`() {
        val emergencyVet = place(name = "Hospital Veterinário 24h")
        val places = FakePlacesPort(nearby = listOf(emergencyVet))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query(category = PlaceCategory.VETERINARY))

        val item = result.locations[0]
        assertEquals(true, item.hasEmergency)
        assertTrue(item.services.contains("emergency"))
        assertNull(item.hasGrooming)
    }

    @Test
    fun `search does not fetch full opening hours (deferred to details)`() {
        val places = FakePlacesPort(nearby = listOf(place()))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), places, FakePlacesCachePort())

        val result = service.search(query())

        val item = result.locations[0]
        assertTrue(item.isOpen)
        assertFalse(item.openingHours.monday.isOpen)
    }

    @Test
    fun `getDetails throws NotFoundException when place is missing`() {
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), FakePlacesPort(detail = null), FakePlacesCachePort())
        assertThrows(NotFoundException::class.java) { service.getDetails("missing") }
    }

    @Test
    fun `getDetails returns details from the port`() {
        val details = PlaceDetails(
            placeId = "place-1",
            description = "Descrição",
            phone = "+55 11 99999-0000",
            website = "https://example.com",
            googleMapsUrl = "https://maps.google.com/?cid=1",
            types = listOf("pet_store"),
            vicinity = "Bairro B",
            priceLevel = 1,
            businessStatus = "OPERATIONAL",
            openingHours = null,
            photoReferences = listOf("ref-1")
        )
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), FakePlacesPort(detail = details), FakePlacesCachePort())

        val result = service.getDetails("place-1")

        assertEquals("Descrição", result.description)
        assertEquals("https://example.com", result.website)
    }

    @Test
    fun `getReviews delegates to the port`() {
        val reviews = listOf(PlaceReview("Autor", 5, "Ótimo", "há 1 semana", null))
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), FakePlacesPort(reviewList = reviews), FakePlacesCachePort())

        assertEquals(1, service.getReviews("place-1").size)
    }

    @Test
    fun `getPhoto delegates to the port`() {
        val photo = PlacePhoto(byteArrayOf(1, 2, 3), "image/jpeg")
        val service = LocationAppService(FakeGeocodingPort(saoPaulo), FakePlacesPort(photo = photo), FakePlacesCachePort())

        val result = service.getPhoto("ref", 800)

        assertEquals("image/jpeg", result.contentType)
        assertEquals(3, result.bytes.size)
    }
}
