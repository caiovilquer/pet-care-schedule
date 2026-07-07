package dev.vilquer.petcarescheduler.usecase.result

// Classe comum (não `data class`) de propósito: ByteArray quebra equals/hashCode
// gerados automaticamente (comparação por referência, não por conteúdo).
class PlacePhoto(val bytes: ByteArray, val contentType: String)
