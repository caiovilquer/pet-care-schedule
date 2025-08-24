package dev.vilquer.petcarescheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = ["dev.vilquer.petcarescheduler"]
)
@EnableScheduling
class PetCareSchedulerApplication

fun main(args: Array<String>) {
    runApplication<PetCareSchedulerApplication>(*args)
}
