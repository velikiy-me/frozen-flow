/*
 *  Copyright © 2026 Vladimir Velikiy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.velikiy.frozenflow.content.xml

import me.velikiy.frozenflow.content.*
import org.w3c.dom.*
import java.io.*
import java.nio.charset.*
import javax.xml.parsers.*
import javax.xml.transform.*
import javax.xml.transform.dom.*
import javax.xml.transform.stream.*

private const val INDENT_NUMBER = 4

class Xml private constructor(
    val document: Document,
) : Content, ContentAccessor by XmlContentAccessor(document) {

    private val normalizedXml: ByteArray by lazy {
        toByteArray(format = false, normalize = true)
    }

    override fun toString(
        charset: Charset,
        format: Boolean,
        normalize: Boolean,
    ): String {
        return StringWriter().use { writer ->
            transform(StreamResult(writer), format, normalize)
            writer.toString().trimTrailingNewlines()
        }
    }

    override fun toString() =
        toString(Charsets.UTF_8, format = false, normalize = false)

    override fun toByteArray(
        format: Boolean,
        normalize: Boolean,
    ): ByteArray {
        return ByteArrayOutputStream().use { os ->
            transform(StreamResult(os), format, normalize)
            os.toByteArray().trimTrailingNewlines()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Xml

        return normalizedXml.contentEquals(other.normalizedXml)
    }

    override fun hashCode(): Int {
        return normalizedXml.contentHashCode()
    }

    private fun transform(outputTarget: Result, format: Boolean, normalize: Boolean) {
        val doc = if (format || normalize) {
            (document.cloneNode(true) as Document)
        } else {
            document
        }.stripWhitespace()

        if (normalize) {
            normalizeNode(doc.documentElement)
        }
        getTransformer(format).transform(DOMSource(doc), outputTarget)
    }

    companion object : ContentFactory<Xml> {

        private val documentFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }

        private val transformerFactory = TransformerFactory.newInstance()

        private val formattingTransformerFactory = TransformerFactory.newInstance().apply {
            setAttribute("indent-number", INDENT_NUMBER)
        }

        override fun parse(content: String, charset: Charset) =
            Xml(documentFactory.newDocumentBuilder().parse(content.byteInputStream(charset)))

        override fun parse(content: ByteArray) =
            Xml(documentFactory.newDocumentBuilder().parse(content.inputStream()))

        private fun getTransformer(format: Boolean): Transformer =
            if (format) {
                formattingTransformerFactory.newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "$INDENT_NUMBER")
                }
            } else {
                transformerFactory.newTransformer()
            }
    }
}

private fun Document.stripWhitespace(): Document {
    stripWhitespaceNodes(documentElement)
    return this
}

private fun stripWhitespaceNodes(node: Node) {
    val toRemove = mutableListOf<Node>()
    var child = node.firstChild

    while (child != null) {
        if (child.nodeType == Node.TEXT_NODE && child.textContent.isBlank()) {
            toRemove.add(child)
        } else if (child.nodeType == Node.ELEMENT_NODE) {
            stripWhitespaceNodes(child)
        }
        child = child.nextSibling
    }

    toRemove.forEach { node.removeChild(it) }
}

private fun ByteArray.trimTrailingNewlines(): ByteArray {
    var end = size
    while (end > 0 && (this[end - 1] == '\n'.code.toByte() || this[end - 1] == '\r'.code.toByte())) {
        end--
    }
    return copyOf(end)
}

private fun String.trimTrailingNewlines(): String =
    trimEnd { it == '\n' || it == '\r' }

private fun normalizeNode(node: Element) {
    // Sort attributes
    val attributes = mutableListOf<Attr>()
    for (i in 0 until node.attributes.length) {
        attributes.add(node.attributes.item(i) as Attr)
    }
    attributes.sortedBy { it.name }.forEach {
        node.removeAttributeNode(it)
        node.setAttributeNode(it)
    }

    // Sort child elements
    val children = mutableListOf<Node>()
    var child = node.firstChild
    while (child != null) {
        children.add(child)
        child = child.nextSibling
    }

    val normalizedChildren = normalizeChildNodes(children)

    children.forEach { node.removeChild(it) }
    normalizedChildren.forEach { node.appendChild(it) }

    // Recursively normalize child elements
    normalizedChildren.forEach { child ->
        if (child.nodeType == Node.ELEMENT_NODE) {
            normalizeNode(child as Element)
        }
    }
}

@Suppress("MagicNumber")
private fun normalizeChildNodes(children: List<Node>): List<Node> =
    children.filter { child ->
        child.nodeType != Node.TEXT_NODE || child.textContent.isNotBlank()
    }.sortedWith(
        compareBy<Node> {
            when (it.nodeType) {
                Node.ELEMENT_NODE -> 0
                Node.TEXT_NODE -> 1
                Node.CDATA_SECTION_NODE -> 2
                Node.COMMENT_NODE -> 3
                else -> 4
            }
        }.thenBy {
            if (it.nodeType == Node.ELEMENT_NODE) it.nodeName else ""
        }
    )
