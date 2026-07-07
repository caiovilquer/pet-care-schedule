package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.usecase.result.GeoLocation

fun interface GeocodingPort {
    /** @return null se o CEP não corresponder a nenhum endereço conhecido. */
    fun geocode(zipCode: String): GeoLocation?
}
