package dev.buecherregale.ebook_reader.core.dom.epub

object EPubConstants {
    /* FILE NAMES */
    const val MIMETYPE: String = "mimetype"
    const val CONTAINER_XML: String = "META-INF/container.xml"

    /* CONTENT */
    const val CONTENT_TYPE: String = "application/epub+zip"
    const val PREFERRED_IDENTIFIER_SCHEME = "MOBI-ASIN"
    const val COVER_XML_ID = "cover"

    /* NAMESPACES */
    const val OPF_NAMESPACE = "http://www.idpf.org/2007/opf"
    const val CONTAINER_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:container"
}