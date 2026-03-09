package dev.buecherregale.ebook_reader.core.dom.epub.xml_structs

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("item")
internal data class Item(
    @XmlElement(false)
    val id: String,
    @XmlElement(false)
    val href: String,
    @XmlElement(false)
    @XmlSerialName("media-type")
    val mediaType: String
)