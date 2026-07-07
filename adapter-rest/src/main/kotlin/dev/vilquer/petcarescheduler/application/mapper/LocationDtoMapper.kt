package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.usecase.command.NearbySearchQuery
import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import org.springframework.stereotype.Component

@Component
class LocationDtoMapper {
    fun toPetshopQuery(zipCode: String, radiusKm: Double, isOpenNow: Boolean, sortBy: String): NearbySearchQuery =
        NearbySearchQuery(zipCode, radiusKm, PlaceCategory.PETSHOP, isOpenNow, sortBy)

    fun toVeterinaryQuery(zipCode: String, radiusKm: Double, isOpenNow: Boolean, sortBy: String): NearbySearchQuery =
        NearbySearchQuery(zipCode, radiusKm, PlaceCategory.VETERINARY, isOpenNow, sortBy)
}
