package dev.buecherregale.ebook_reader.core.dom.epub

import dev.buecherregale.ebook_reader.core.dom.Branch
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.dom.Node
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Chapter is just a simple branching node but keeping some of the original epub structure
 *
 * @param id                    the new id for the node
 * @param children              the child nodes
 * @param originalZipPath       the path to the chapters xhtml in the zip
 * @param originalLinkAnchor    identifier for a link anchor (targeted by things like `<a>`) = XML id
 */
@Serializable
internal data class Chapter(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    @Transient
    val originalZipPath: String = "",
) : Branch

/**
 * Rename the Document to specify EPub.
 *
 * CONTRACT: This parser creates ONE EPub/Document per run.
 * The documents' children are all [Chapter]s.
 */
typealias EPub = Document