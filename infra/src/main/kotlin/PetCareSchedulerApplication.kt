package dev.vilquer.petcarescheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["dev.vilquer.petcarescheduler"]
)
class PetCareSchedulerApplication

fun main(args: Array<String>) {
    runApplication<PetCareSchedulerApplication>(*args)
}
