package dev.buecherregale.ebook_reader.core.dom.epub.xml_structs

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("itemref")
internal data class ItemRef(
    val idref: String
)