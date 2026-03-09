package dev.buecherregale.ebook_reader.core.dom.epub.xml_structs

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

private object Ns {
    const val DC = "http://purl.org/dc/elements/1.1/"
    const val OPF = "http://www.idpf.org/2007/opf"
}

@Serializable
@XmlSerialName("package", namespace = Ns.OPF)
internal data class Package(
    @XmlElement(true)
    @XmlSerialName("metadata")
    val metadata: Metadata,
    @XmlElement(true)
    @XmlSerialName("manifest")
    val manifest: Manifest,
    val spine: Spine,
)

@Serializable
@XmlSerialName("manifest")
internal data class Manifest(
    @XmlElement(true)
    @XmlSerialName("item")
    val items: List<Item>,
)

@Serializable
@XmlSerialName("spine")
internal data class Spine(
    @XmlElement(false)
    @XmlSerialName("page-progression-direction")
    val pageProgressionDirection: String = "ltr",
    @XmlElement(false)
    val toc: String,
    @XmlElement(true)
    @XmlSerialName("itemref")
    val itemRefs: List<ItemRef>
)

/**
 * While containing the whole [Ns.DC] spec, most of it is going unused.
 * Metadata is also a common spot to inject other additional information (e.g. calibre)
 * TODO: additional mappings for remaining fields
 */
@Serializable
@XmlSerialName("metadata")
internal data class Metadata(
    @XmlElement(true)
    @XmlSerialName("contributor", Ns.DC, "dc")
    val contributor: String?,
    @XmlElement(true)
    @XmlSerialName(value = "coverage", namespace = Ns.DC, prefix = "dc")
    val coverage: String?,
    @XmlElement(true)
    @XmlSerialName(value = "creator", namespace = Ns.DC, prefix = "dc")
    val creator: List<Creator> = emptyList(),
    @XmlElement(true)
    @XmlSerialName(value = "date", namespace = Ns.DC, prefix = "dc")
    val date: String?,
    @XmlElement(true)
    @XmlSerialName(value = "description", namespace = Ns.DC, prefix = "dc")
    val description: String?,
    @XmlElement(true)
    @XmlSerialName(value = "format", namespace = Ns.DC, prefix = "dc")
    val format: String?,
    @XmlElement(true)
    @XmlSerialName(value = "identifier", namespace = Ns.DC, prefix = "dc")
    val identifiers: List<Identifier> = emptyList(),
    @XmlElement(true)
    @XmlSerialName(value = "language", namespace = Ns.DC, prefix = "dc")
    val language: String,
    @XmlElement(true)
    @XmlSerialName(value = "publisher", namespace = Ns.DC, prefix = "dc")
    val publisher: String?,
    @XmlElement(true)
    @XmlSerialName(value = "relation", namespace = Ns.DC, prefix = "dc")
    val relations: List<String> = emptyList(),
    @XmlElement(true)
    @XmlSerialName(value = "rights", namespace = Ns.DC, prefix = "dc")
    val rights: String?,
    @XmlElement(true)
    @XmlSerialName(value = "source", namespace = Ns.DC, prefix = "dc")
    val source: String?,
    @XmlElement(true)
    @XmlSerialName(value = "subject", namespace = Ns.DC, prefix = "dc")
    val subject: String?,
    @XmlElement(true)
    @XmlSerialName(value = "title", namespace = Ns.DC, prefix = "dc")
    val title: String,
    @XmlElement(true)
    @XmlSerialName(value = "type", namespace = Ns.DC, prefix = "dc")
    val type: String?,
)

@Serializable
@XmlSerialName("creator", Ns.DC, "dc")
internal data class Creator(
    @XmlValue(true)
    val name: String,

    @XmlElement(false)
    @XmlSerialName("file-as", Ns.OPF, "opf")
    val fileAs: String? = null,

    @XmlElement(false)
    @XmlSerialName("role", Ns.OPF, "opf")
    val role: String? = null
)

@Serializable
@XmlSerialName("identifier", Ns.DC, "dc")
internal data class Identifier(
    @XmlValue(true)
    val value: String,

    @XmlElement(false)
    val id: String? = null,

    @XmlElement(false)
    @XmlSerialName("scheme", Ns.OPF, "opf")
    val scheme: String? = null
)