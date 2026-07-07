package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.NearbySearchQuery
import dev.vilquer.petcarescheduler.usecase.result.LocationSearchResult

fun interface SearchLocationsUseCase {
    fun search(query: NearbySearchQuery): LocationSearchResult
}
