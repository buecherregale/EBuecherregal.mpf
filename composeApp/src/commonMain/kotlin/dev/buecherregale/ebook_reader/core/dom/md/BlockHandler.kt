package dev.buecherregale.ebook_reader.core.dom.md

import dev.buecherregale.ebook_reader.core.dom.*

interface BlockHandler {
    val priority: Int get() = 0

    fun canParse(lines: List<String>, index: Int): Boolean

    suspend fun parse(
        lines: List<String>,
        index: Int,
        context: ParseContext,
    ): BlockParseResult
}

data class BlockParseResult(
    val node: Node?,
    val linesConsumed: Int,
)

class HeadingBlockHandler : BlockHandler {
    override val priority = 20
    private val regex = Regex("""^(#{1,6})\s+(.+?)(?:\s+#+)?\s*$""")

    override fun canParse(lines: List<String>, index: Int) =
        regex.containsMatchIn(lines[index])

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val m = regex.find(lines[index]) ?: return BlockParseResult(null, 1)
        return BlockParseResult(
            node = Heading(
                id = nextId(),
                level = m.groupValues[1].length,
                children = context.parseInline(m.groupValues[2]).toMutableList(),
                originalLinkAnchor = m.groupValues[2]
                    .lowercase()
                    .replace(Regex("""[^\w\s-]"""), "")
                    .replace(Regex("""\s+"""), "-"),
            ),
            linesConsumed = 1,
        )
    }
}

class SetextHeadingBlockHandler : BlockHandler {
    override val priority = 21

    override fun canParse(lines: List<String>, index: Int): Boolean {
        if (lines[index].isBlank() || index + 1 >= lines.size) return false
        return lines[index + 1].matches(Regex("""^[=\-]+\s*$"""))
    }

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val level = if (lines[index + 1].trimStart().startsWith("=")) 1 else 2
        return BlockParseResult(
            node = Heading(
                id = nextId(),
                level = level,
                children = context.parseInline(lines[index]).toMutableList(),
            ),
            linesConsumed = 2,
        )
    }
}

open class FencedCodeBlockHandler : BlockHandler {
    override val priority = 30
    private val fenceRegex = Regex("""^(`{3,}|~{3,})""")

    override fun canParse(lines: List<String>, index: Int) =
        fenceRegex.containsMatchIn(lines[index])

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val fence = fenceRegex.find(lines[index])!!.groupValues[1]
        var i = index + 1
        while (i < lines.size && !lines[i].startsWith(fence)) i++
        return BlockParseResult(null, linesConsumed = i - index + 1)
    }
}

open class IndentedCodeBlockHandler : BlockHandler {
    override val priority = 5

    override fun canParse(lines: List<String>, index: Int) =
        lines[index].startsWith("    ") || lines[index].startsWith("\t")

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        var i = index
        while (i < lines.size &&
            (lines[i].startsWith("    ") || lines[i].startsWith("\t") || lines[i].isBlank())
        ) i++
        return BlockParseResult(null, maxOf(1, i - index))
    }
}

open class HorizontalRuleBlockHandler : BlockHandler {
    override val priority = 25
    private val regex = Regex("""^(\*{3,}|-{3,}|_{3,})\s*$""")

    override fun canParse(lines: List<String>, index: Int) =
        regex.matches(lines[index].trim())

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult =
        BlockParseResult(null, 1)
}

open class BlockquoteBlockHandler : BlockHandler {
    override val priority = 10

    override fun canParse(lines: List<String>, index: Int) =
        lines[index].trimStart().startsWith(">")

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        var i = index
        while (i < lines.size && (lines[i].trimStart().startsWith(">") || lines[i].isBlank())) i++
        return BlockParseResult(null, maxOf(1, i - index))
    }
}

class ListBlockHandler : BlockHandler {
    override val priority = 15

    private val unordered = Regex("""^(\s*)[*\-+]\s+(.*)""")
    private val ordered = Regex("""^(\s*)\d+[.)]\s+(.*)""")

    private fun isListStart(line: String) = unordered.containsMatchIn(line) || ordered.containsMatchIn(line)
    private fun isOrdered(line: String) = ordered.containsMatchIn(line)

    override fun canParse(lines: List<String>, index: Int) = isListStart(lines[index])

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val listBlock = ListBlock(id = nextId(), ordered = isOrdered(lines[index]))
        var i = index

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> {
                    if (i + 1 < lines.size && isListStart(lines[i + 1])) {
                        i++; continue
                    }
                    break
                }

                isListStart(line) -> {
                    val content = unordered.find(line)?.groupValues?.getOrElse(2) { "" }
                        ?: ordered.find(line)?.groupValues?.getOrElse(2) { "" }
                        ?: ""
                    listBlock.children += ListItem(
                        id = nextId(),
                        children = context.parseInline(content.trim()).toMutableList(),
                    )
                    i++
                }

                line.startsWith("    ") || line.startsWith("\t") -> i++
                else -> break
            }
        }

        return BlockParseResult(listBlock, maxOf(1, i - index))
    }
}

open class ImageBlockHandler(private val resolveResources: Boolean = false) : BlockHandler {
    override val priority = 16
    private val regex = Regex("""^!\[([^]]*)]\(([^)]+)\)\s*$""")

    override fun canParse(lines: List<String>, index: Int) =
        regex.containsMatchIn(lines[index].trim())

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val m = regex.find(lines[index].trim()) ?: return BlockParseResult(null, 1)
        val alt = m.groupValues[1]
        val src = m.groupValues[2]

        val resolvedSrc = if (resolveResources) resolveSrc(src, context) else src

        return BlockParseResult(
            node = ImageBlock(
                id = nextId(),
                image = Image(id = nextId(), src = resolvedSrc, alt = alt),
            ),
            linesConsumed = 1,
        )
    }

    protected open suspend fun resolveSrc(src: String, context: ParseContext): String = try {
        val ref = context.baseFile.resolve(src)
        val bytes = context.fileService.readBytes(ref)
        val id = nextId()
        context.resourceRepository.save(id, bytes)
        id
    } catch (_: Exception) {
        src
    }
}

class ParagraphBlockHandler(
    private val breakOn: List<(String) -> Boolean> = emptyList(),
) : BlockHandler {
    override val priority = 0

    override fun canParse(lines: List<String>, index: Int) = true

    override suspend fun parse(lines: List<String>, index: Int, context: ParseContext): BlockParseResult {
        val content = mutableListOf<String>()
        var i = index
        while (i < lines.size && lines[i].isNotBlank() && breakOn.none { it(lines[i]) }) {
            content += lines[i].trim()
            i++
        }
        return BlockParseResult(
            node = Paragraph(
                id = nextId(),
                children = context.parseInline(content.joinToString(" ")).toMutableList(),
            ),
            linesConsumed = maxOf(1, i - index),
        )
    }
}

class BlockHandlerRegistry {
    private val handlers = mutableListOf<BlockHandler>()

    fun register(handler: BlockHandler) {
        handlers += handler
    }

    fun registerAll(handlers: List<BlockHandler>) {
        this.handlers.addAll(handlers)
    }

    fun collect(): List<BlockHandler> {
        return handlers.sortedByDescending { it.priority }
    }

    companion object {
        fun default(): BlockHandlerRegistry = BlockHandlerRegistry().apply {
            registerAll(
                listOf(
                    FencedCodeBlockHandler(),
                    SetextHeadingBlockHandler(),
                    HeadingBlockHandler(),
                    HorizontalRuleBlockHandler(),
                    ListBlockHandler(),
                    ImageBlockHandler(),
                    BlockquoteBlockHandler(),
                    IndentedCodeBlockHandler(),
                    ParagraphBlockHandler(),
                )
            )
        }
    }
}
