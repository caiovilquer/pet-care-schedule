package dev.vilquer.petcarescheduler.infra

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Ponto de ancoragem para @DataJpaTest: fornece um @SpringBootConfiguration
 * cujo pacote (dev.vilquer.petcarescheduler.infra) é ancestral das entidades
 * e repositórios deste módulo, permitindo que a auto-detecção de pacote base
 * encontre os @Entity/JpaRepository sem depender do módulo bootstrap.
 */
@SpringBootApplication
class PersistenceTestApplication
