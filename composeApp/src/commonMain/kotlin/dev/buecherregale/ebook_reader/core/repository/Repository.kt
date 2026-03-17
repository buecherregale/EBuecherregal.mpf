package dev.buecherregale.ebook_reader.core.repository

import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import kotlinx.io.Source

/**
 * A generic repository aimed at persisting data.
 *
 * @param Key the type of unique key to access/save data by
 * @param T the type to persist
 */
interface Repository<Key, T> {
    /**
     * Loads all data from the repository.
     */
    suspend fun loadAll(): List<T>

    /**
     * Loads data with the given key.
     */
    suspend fun load(key: Key): T?

    /**
     * Saves data at the given key.
     */
    suspend fun save(key: Key, value: T): T

    /**
     * Deletes data with the given key.
     */
    suspend fun delete(key: Key)
}

interface FileBasedRepository<Key> : Repository<Key, ByteArray> {
    fun getFile(key: Key): FileRef
    suspend fun loadSource(key: Key): Source?
}


class FileRepository<Key>(
    private val keyToFilename: (Key) -> String,
    private val storeInDir: FileRef,
    private val fileService: FileService,
) : FileBasedRepository<Key> {

    override suspend fun loadAll(): List<ByteArray> {
        Logger.i { "loading all files from '${storeInDir.path}'" }
        return fileService.listChildren(storeInDir)
            .map { fileService.readBytes(it) }
    }

    override suspend fun load(key: Key): ByteArray? {
        val file = storeInDir.resolve(keyToFilename(key))

        Logger.i { "loading resource '$key' from file '${storeInDir.path}'" }
        if (!fileService.exists(file)) return null

        return fileService.readBytes(file)
    }

    override suspend fun save(key: Key, value: ByteArray): ByteArray {
        val file = storeInDir.resolve(keyToFilename(key))

        Logger.i { "saving resource '$key' to file '${storeInDir.path}'" }
        fileService.write(file, value)
        return load(key)!! // should def exist now
    }

    override suspend fun delete(key: Key) {
        Logger.i { "deleting resource '$key' with file '${keyToFilename(key)}'" }
        fileService.delete(storeInDir.resolve(keyToFilename(key)))
    }

    override fun getFile(key: Key): FileRef = storeInDir.resolve(keyToFilename(key))

    override suspend fun loadSource(key: Key): Source? {
        val file = getFile(key)

        Logger.i { "loading resource '$key' from file '${storeInDir.path}' as source" }
        if (!fileService.exists(file)) return null
        return fileService.open(file)
    }
}
