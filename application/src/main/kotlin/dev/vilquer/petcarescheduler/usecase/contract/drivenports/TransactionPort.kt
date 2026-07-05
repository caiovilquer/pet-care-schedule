package dev.vilquer.petcarescheduler.usecase.contract.drivenports

interface TransactionPort {
    /** Executa [block] de forma atômica: tudo ou nada. */
    fun <T> execute(block: () -> T): T
}
