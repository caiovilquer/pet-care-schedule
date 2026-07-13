package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurement
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecord
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthMeasurementJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthRecordJpa

fun HealthRecord.toJpa() = HealthRecordJpa().also {
    it.id = id.value; it.version = version; it.householdId = householdId.value; it.tutorId = tutorId.value; it.petId = petId.value; it.type = type
    it.occurredAt = occurredAt; it.title = title; it.notes = notes; it.productName = productName; it.dosage = dosage
    it.batchNumber = batchNumber; it.professionalName = professionalName; it.clinicName = clinicName
    it.costAmount = costAmount; it.currency = currency; it.createdByTutorId = createdByTutorId.value
    it.createdAt = createdAt; it.updatedAt = updatedAt
}

fun HealthRecordJpa.toDomain() = HealthRecord(
    id = HealthRecordId(id), version = version, householdId = HouseholdId(householdId), tutorId = TutorId(tutorId),
    petId = PetId(petId), type = type, occurredAt = occurredAt, title = title, notes = notes, productName = productName,
    dosage = dosage, batchNumber = batchNumber, professionalName = professionalName, clinicName = clinicName,
    costAmount = costAmount, currency = currency, createdByTutorId = TutorId(createdByTutorId), createdAt = createdAt, updatedAt = updatedAt,
)

fun HealthMeasurement.toJpa() = HealthMeasurementJpa().also {
    it.id = id.value; it.version = version; it.householdId = householdId.value; it.tutorId = tutorId.value; it.petId = petId.value; it.type = type
    it.value = value; it.unit = unit; it.measuredAt = measuredAt; it.notes = notes
    it.createdByTutorId = createdByTutorId.value; it.createdAt = createdAt; it.updatedAt = updatedAt
}

fun HealthMeasurementJpa.toDomain() = HealthMeasurement(
    id = HealthMeasurementId(id), version = version, householdId = HouseholdId(householdId), tutorId = TutorId(tutorId),
    petId = PetId(petId), type = type, value = value, unit = unit, measuredAt = measuredAt, notes = notes,
    createdByTutorId = TutorId(createdByTutorId), createdAt = createdAt, updatedAt = updatedAt,
)
