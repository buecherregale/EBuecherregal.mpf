package dev.buecherregale.ebook_reader.core.dom.md

import androidx.compose.ui.text.intl.Locale
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.BookMetadata
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun nextId() = Uuid.generateV4().toString()

class ParseContext(
    val baseFile: FileRef,
    val fileService: FileService,
    val resourceRepository: ResourceRepository,
    private val parser: MarkdownDomParser,
) {

    fun parseInline(text: String): List<Inline> =
        parser.parseInlineText(text, this)

    suspend fun parseBlocks(lines: List<String>): List<Node> =
        parser.parseBlockLines(lines, this)
}

/**
 * Parser for `.md` Markdown files and folders containing them.
 */
class MarkdownDomParser(
    blockHandlerRegistry: BlockHandlerRegistry = BlockHandlerRegistry.default(),
    inlineHandlerRegistry: InlineHandlerRegistry = InlineHandlerRegistry.default(),
) : BookParser {
    private val blockHandlers = blockHandlerRegistry.collect()
    private val inlineHandlers = inlineHandlerRegistry.collect()

    override suspend fun canParse(
        file: FileRef,
        fileService: FileService
    ): Boolean {
        val meta = fileService.getMetadata(file)
        return meta.extension == ".md" || meta.isDirectory && fileService.listChildren(file)
            .any { fileService.getMetadata(it).extension == ".md" }
    }

    /**
     * Parses either a single file or a folder of Markdown files, denoted by `.md` extension.
     *
     * Does not check subfolders recursively. Only direct children are parsed.
     */
    override suspend fun parse(
        file: FileRef,
        fileService: FileService,
        resourceRepository: ResourceRepository,
        targetId: String
    ): Pair<Book, Document> {
        val metadata = fileService.getMetadata(file)
        val book = Book(
            id = targetId,
            progress = 0.0,
            metadata = BookMetadata(
                title = metadata.name,
                author = "who knows",
                isbn = "",
                language = Locale.current
            )
        )
        if (!metadata.isDirectory) {
            return book to parse(fileService.read(file), file, fileService, resourceRepository)
        }
        return book to Document(
            id = nextId(),
            children = fileService.listChildren(file)
                .filter { child ->
                    val meta = fileService.getMetadata(child)
                    !meta.isDirectory && meta.extension == ".md"
                }
                .map { child -> parse(fileService.read(child), file, fileService, resourceRepository) }
                .map { it as Node }
                .toMutableList(),
        )
    }

    override suspend fun parseCover(
        file: FileRef,
        fileService: FileService
    ): ByteArray? {
        Logger.i { "cover images for markdown are not supported" }
        return null
    }

    /**
     * Parses the given string to a DOM representation.
     */
    suspend fun parse(
        markdown: String,
        baseFile: FileRef,
        fileService: FileService,
        resourceRepository: ResourceRepository,
    ): Document {
        val context = ParseContext(
            baseFile = baseFile,
            fileService = fileService,
            resourceRepository = resourceRepository,
            parser = this,
        )
        val lines = markdown.lines()
        val children = parseBlockLines(lines, context)
        return Document(
            id = nextId(),
            children = children.toMutableList()
        )
    }

    internal suspend fun parseBlockLines(
        lines: List<String>,
        context: ParseContext
    ): List<Node> {
        val nodes = mutableListOf<Node>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].isBlank()) {
                i++; continue
            }

            val rule = this@MarkdownDomParser.blockHandlers.firstOrNull { it.canParse(lines, i) }
            if (rule != null) {
                val result = rule.parse(lines, i, context)
                result.node?.let { nodes += it }
                i += maxOf(1, result.linesConsumed)
            } else {
                i++
            }
        }
        return nodes
    }

    internal fun parseInlineText(
        text: String,
        context: ParseContext
    ): List<Inline> {
        val result = mutableListOf<Inline>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            var earliest: InlineMatch? = null
            var winner: InlineHandler? = null

            for (rule in this@MarkdownDomParser.inlineHandlers) {
                val m = rule.findMatch(remaining) ?: continue
                if (earliest == null
                    || m.start < earliest.start
                    || (m.start == earliest.start && rule.priority > (winner?.priority ?: 0))
                ) {
                    earliest = m
                    winner = rule
                }
            }

            if (earliest == null || winner == null) {
                result += Text(
                    id = nextId(),
                    text = remaining
                )
                break
            }

            if (earliest.start > 0) {
                result += Text(
                    id = nextId(),
                    text = remaining.substring(0, earliest.start)
                )
            }

            result += winner.parse(remaining, earliest, context)

            remaining = remaining.substring(earliest.end)
        }

        return result
    }
}

