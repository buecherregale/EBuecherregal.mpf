package dev.buecherregale.ebook_reader.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val progress: Double,
    val metadata: BookMetadata
)