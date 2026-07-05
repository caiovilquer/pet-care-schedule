package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.result.*

fun Pet.toSummary(): PetSummary =
    PetSummary(id = id!!, name = name, species = species, photoUrl = photoUrl)

// Pet e Tutor referenciam seus filhos por ID, não por composição; o read model
// completo é montado no service a partir de uma consulta explícita ao filho.
fun Pet.toDetailResult(events: List<Event>): PetDetailResult = PetDetailResult(
    id = id!!,
    name = name,
    species = species,
    breed = breed,
    birthdate = birthdate!!,
    photoUrl = photoUrl,
    events = events.map { ev ->
        PetDetailResult.EventInfo(
            id = ev.id!!,
            type = ev.type,
            dateStart = ev.dateStart
        )
    }
)

fun Tutor.toDetailResult(pets: List<Pet>): TutorDetailResult = TutorDetailResult(
    id = id!!,
    firstName = firstName,
    lastName = lastName,
    email = email.value,
    phoneNumber = phoneNumber?.e164,
    avatar = avatar,
    pets = pets.map { pet ->
        TutorDetailResult.PetInfo(
            id = pet.id!!,
            name = pet.name,
            species = pet.species,
            photoUrl = pet.photoUrl
        )
    }
)

fun Event.toDetailResult(): EventDetailResult = EventDetailResult(
    id = id!!,
    type = type,
    description = description,
    dateStart = dateStart,
    recurrence = recurrence,
    status = status,
)

fun Event.toSummary(): EventSummary = EventSummary(
    id = id!!,
    type = type,
    status = status,
    description = description,
    dateStart = dateStart,
    petId = petId!!
)