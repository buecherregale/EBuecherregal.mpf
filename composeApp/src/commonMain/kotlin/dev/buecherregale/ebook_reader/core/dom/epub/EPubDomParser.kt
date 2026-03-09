package dev.buecherregale.ebook_reader.core.dom.epub

import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.dom.epub.xml_structs.Container
import dev.buecherregale.ebook_reader.core.dom.epub.xml_structs.Item
import dev.buecherregale.ebook_reader.core.dom.epub.xml_structs.Package
import dev.buecherregale.ebook_reader.core.dom.xml.XmlDomParser
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.BookMetadata
import dev.buecherregale.ebook_reader.core.exception.EPubParseException
import dev.buecherregale.ebook_reader.core.exception.EPubSyntaxException
import dev.buecherregale.ebook_reader.core.language.normalizeLanguage
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.core.service.filesystem.ZipFileRef
import dev.buecherregale.ebook_reader.ui.util.UrlUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig

/**
 * Parser for EPub books.
 * A EPub file is a zip folder following a specific structure.
 *
 * Parses chapter by chapter opening and validating the zip content.
 *
 * Resources are extracted from the zip and placed in a repository.
 */
object EPubDomParser : BookParser {

    override suspend fun canParse(
        file: FileRef,
        fileService: FileService
    ): Boolean {
        try {
            val zip = fileService.readZip(file)
            val entry = zip.getEntry(EPubConstants.MIMETYPE)
            if (entry == null) {
                Logger.i { "cannot parse epub: ${EPubConstants.MIMETYPE} file not found" }
                return false
            }
            if (entry.open()
                    .readByteArray()
                    .decodeToString()
                    .trim() == EPubConstants.CONTENT_TYPE
            ) {
                Logger.i { "cannot parse epub: content-type is invalid" }
                return false
            }

            return true
        } catch (e: IOException) {
            Logger.i { "cannot parse epub: error while reading as zip: ${e.message}" }
            Logger.d { "stacktrace:\n$e" }
            return false
        }
    }

    /**
     * Parses the EPub zip file by reading the metadata providing XMLs before reading the `spine` of the book, parsing the chapters.
     * Internal resources are saved to the [dev.buecherregale.ebook_reader.core.dom.ResourceRepository]. Links like image sources or to different chapters/headings are rewritten to fit the DOM structure.
     *
     * The resulting document only has [Chapter] as children.
     *
     * @see [EPub]
     * @see [Chapter]
     */
    override suspend fun parse(
        file: FileRef,
        fileService: FileService,
        resourceRepository: ResourceRepository,
        targetId: String
    ): Pair<Book, Document> {
        val zip = fileService.readZip(file)
        val container = readContainer(zip)
        val pkg = readPackage(zip, container.rootfiles.rootfile[0].fullPath)
        val chapters = pkg.spine.itemRefs
            .map { EPubXmlResolver.resolveId(pkg, it.idref) }
            .map {
                readChapter(
                    zip = zip,
                    pathToChapter = it.href
                )
            }
            .map { it as Node }
            .toMutableList()
        val document = EPub(
            id = targetId,
            children = chapters
        )
        postProcessors(zip, resourceRepository).forEach { it.process(document) }

        return Book(
            id = targetId,
            progress = 0.0,
            metadata = readMetadata(pkg)
        ) to document
    }

    override suspend fun parseCover(
        file: FileRef,
        fileService: FileService
    ): ByteArray? {
        val zip = fileService.readZip(file)
        val container = readContainer(zip)
        val pkg = readPackage(zip, container.rootfiles.rootfile[0].fullPath)

        return pkg.manifest.items
            .filter { (id, _, _) -> id == EPubConstants.COVER_XML_ID }
            .mapNotNull { item -> zip.getEntry(item.href) }
            .map { it.open().readByteArray() }
            .firstOrNull()
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private val generalXmlParser = XML.v1.invoke {
        policy = DefaultXmlSerializationPolicy {
            unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
        }
    }

    private fun postProcessors(zip: ZipFileRef, resourceRepository: ResourceRepository): List<PostProcessor> =
        listOf(LinkPostProcessor, ImagePostProcessor(zip, resourceRepository))

    private suspend fun readContainer(zip: ZipFileRef): Container {
        val xmlEntry = zip.getEntry(EPubConstants.CONTAINER_XML)
            ?: throw EPubSyntaxException("${EPubConstants.CONTAINER_XML} is missing")
        val xmlString = xmlEntry.open().use {
            it.readByteArray().decodeToString()
        }
        return generalXmlParser.decodeFromString(xmlString)
    }

    private suspend fun readPackage(zip: ZipFileRef, opfPath: String): Package {
        val xmlEntry = zip.getEntry(opfPath)
            ?: throw EPubSyntaxException("rootfile $opfPath not found")

        val xmlString = xmlEntry.open().use {
            it.readByteArray().decodeToString()
        }

        return generalXmlParser.decodeFromString(xmlString)
    }

    private suspend fun readChapter(zip: ZipFileRef, pathToChapter: String): Chapter {
        val entry = zip.getEntry(pathToChapter)
            ?: throw EPubParseException("Missing chapter item at '$pathToChapter'")

        val xmlString = entry.open().readByteArray().decodeToString()
        val document = XmlDomParser.parse(
            xml = xmlString,
        )

        return Chapter(
            id = document.id,
            children = document.children,
            originalZipPath = pathToChapter,
            originalLinkAnchor = pathToChapter.substringAfterLast("/", pathToChapter)
        )
    }

    private fun readMetadata(pkg: Package): BookMetadata {
        val isbn = pkg.metadata.identifiers
            .find { it.scheme == EPubConstants.PREFERRED_IDENTIFIER_SCHEME }
            ?.value
            ?: ""
        return BookMetadata(
            title = pkg.metadata.title,
            language = normalizeLanguage(pkg.metadata.language),
            author = pkg.metadata.creator.first().name,
            isbn = isbn
        )
    }
}

internal interface PostProcessor {
    suspend fun process(document: Document)
}

internal class ImagePostProcessor(
    private val zip: ZipFileRef,
    private val resourceRepository: ResourceRepository
) : PostProcessor {
    override suspend fun process(document: Document) {
        val images: MutableSet<Pair<Chapter, Image>> = mutableSetOf()
        document.children.forEach { child ->
            val chapter = child as? Chapter
                ?: throw EPubSyntaxException("non chapter child of epub document: ${child.id}, anchor: ${child.originalLinkAnchor}")
            chapter.children.forEach {
                it.visit { node ->
                    if (node is Image) images.add(chapter to node)
                }
            }
        }
        val semaphore = Semaphore(8)

        coroutineScope {
            images.map { image ->
                async {
                    semaphore.withPermit {
                        val path = EPubXmlResolver.resolvePath(image.first.originalZipPath, image.second.src)
                        val entry = zip.getEntry(path)
                            ?: throw EPubParseException("missing image at '$path'")

                        val bytes = entry.open().readByteArray()
                        resourceRepository.save(image.second.id, bytes)
                        image.second.src = UrlUtil.BOOK_RESOURCE_PROTOCOL + "://${image.second.id}"
                    }
                }
            }.awaitAll()
        }
    }
}

internal object LinkPostProcessor : PostProcessor {
    override suspend fun process(document: Document) {
        val links: MutableSet<Link> = mutableSetOf()
        document.visit { node ->
            if (node is Link)
                links.add(node)
        }
        links.forEach { link ->
            val path = findPathToLink(document, link)
            if (path != null)
                link.target = pathToUrl(path)
        }
    }

    /**
     * Finds the path to the link by stepping through nodes and checking their [Node.originalLinkAnchor].
     *
     * Requires that [Link.target] is of form `chapterFile?#originalLinkAnchor?` of an element.
     * Also [Chapter.originalLinkAnchor] has to be the `chapterFile`.
     *
     * @param document The document to traverse
     * @param link the link to find
     * @return the path to the link or `null` if the target does not match the requirement
     */
    private fun findPathToLink(document: Document, link: Link): ArrayDeque<String>? {
        val path = ArrayDeque<String>()
        Regex("^(?<chapter>[^#]*)(#(?<anchor>.+))?$").find(link.target)?.let { matchResult ->
            if (matchResult.groups["chapter"] == null && matchResult.groups["anchor"] == null) // both null means no match
                return null
            val chapter = matchResult.groups["chapter"]?.value
                ?: (findChapterToLink(document, matchResult.groups["anchor"]!!.value)?.id
                    ?: throw EPubParseException("no chapter for link target '${link.target}' found"))
            val chapterNode = document.children.find { it.originalLinkAnchor == chapter }
                ?: throw EPubParseException("no chapter for link target '${link.target}' found")
            path.addLast(chapterNode.id)
            if (matchResult.groups["anchor"] == null)
                return path
            path.removeLast()
            val found = findLinkTarget(chapterNode, matchResult.groups["anchor"]!!.value, path)
            if (found)
                return path
        }
        throw EPubParseException("no path for link target '${link.target}' found")
    }

    private fun findChapterToLink(document: Document, targetAnchor: String): Chapter? {
        var found: Chapter? = null
        document.children.forEach { chapter ->
            chapter.visit { node ->
                if (node.originalLinkAnchor == targetAnchor)
                    found = chapter as Chapter
            }
        }
        return found
    }

    private fun findLinkTarget(node: Node, target: String, path: ArrayDeque<String>): Boolean {
        path.addLast(node.id)
        if (node.originalLinkAnchor == target) {
            return true
        }
        if (node is Branch)
            node.children.forEach {
                if (findLinkTarget(it, target, path))
                    return true
            }
        path.removeLast()
        return false
    }

    private fun pathToUrl(path: ArrayDeque<String>): String {
        return UrlUtil.BOOK_LINK_PROTOCOL + "://" + path.joinToString("/")
    }
}

internal object EPubXmlResolver {

    /**
     * Finds an item in the [dev.buecherregale.ebook_reader.core.dom.epub.xml_structs.Manifest] with matching id.
     *
     * @param pkg the opf package to search in
     * @param xmlId the id of the item
     * @return the manifest item
     * @throws EPubParseException if the item is not found
     */
    fun resolveId(pkg: Package, xmlId: String): Item {
        return pkg.manifest.items.find { (itemId, _, _) -> xmlId == itemId }
            ?: throw EPubParseException("could not find item '$xmlId' in manifest")
    }

    /**
     * Resolves the `href` path as a relative path to the `base`, following a unix fs style, '/' separated path.
     *
     * Respects `..` or `.` in the `href`.
     *
     * @param base the path to resolve relative to
     * @param href a relative path to `base`
     * @return the resolved path
     */
    fun resolvePath(base: String, href: String): String {
        val baseParts = base.substringBeforeLast('/', "")
            .split('/')
            .filter { it.isNotEmpty() }
            .toMutableList()
        val refParts = href.split('/')

        for (part in refParts) {
            when (part) {
                "", "." -> Unit
                ".." -> if (baseParts.isNotEmpty()) baseParts.removeAt(baseParts.lastIndex)
                else -> baseParts.add(part)
            }
        }
        return baseParts.joinToString("/")
    }
}