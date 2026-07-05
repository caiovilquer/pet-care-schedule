package dev.vilquer.petcarescheduler.application.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Sem isto, cada instância do bootstrap rodaria os três schedulers de forma
 * independente; num cluster, o lembrete diário e a limpeza de segurança
 * disparariam duplicados por instância (risco já apontado na AUDITORIA.md).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
class SchedulerLockConfig {

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        )
}
