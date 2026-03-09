package dev.buecherregale.ebook_reader.core.dom.epub.xml_structs

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private const val CONTAINER_NS =
    "urn:oasis:names:tc:opendocument:xmlns:container"

@Serializable
@XmlSerialName("container", CONTAINER_NS)
internal data class Container(
    val rootfiles: RootFiles
)

@Serializable
@XmlSerialName("rootfiles")
internal data class RootFiles(
    @XmlElement(true)
    @XmlSerialName("rootfile")
    val rootfile: List<RootFile>
)

@Serializable
@XmlSerialName("rootfile")
internal data class RootFile(

    @XmlElement(false)
    @XmlSerialName("full-path")
    val fullPath: String,

    @XmlElement(false)
    @XmlSerialName("media-type")
    val mediaType: String
)
