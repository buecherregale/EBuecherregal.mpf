package dev.buecherregale.ebook_reader.core.dom

import dev.buecherregale.ebook_reader.core.repository.FileBasedRepository
import dev.buecherregale.ebook_reader.core.repository.FileRepository
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ResourceRepository(
    delegate: FileRepository<String>
) : FileBasedRepository<String> by delegate