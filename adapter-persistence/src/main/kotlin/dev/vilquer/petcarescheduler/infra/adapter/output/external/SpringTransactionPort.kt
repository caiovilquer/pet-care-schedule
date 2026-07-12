package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class SpringTransactionPort(
    transactionManager: PlatformTransactionManager
) : TransactionPort {

    private val template = TransactionTemplate(transactionManager)

    override fun <T> execute(block: () -> T): T {
        var result: Any? = null
        template.executeWithoutResult {
            result = block()
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
