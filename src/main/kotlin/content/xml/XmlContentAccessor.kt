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
import javax.xml.xpath.*

class XmlContentAccessor(
    private val document: Document,
) : ContentAccessor {

    private val xpath = object : InheritableThreadLocal<XPath>() {
        override fun initialValue(): XPath {
            return XPathFactory.newInstance().newXPath()
        }
    }

    override fun exists(ref: String): Boolean {
        val nodeList = readNodes(ref)
        return nodeList.length > 0
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> read(ref: String): T? {
        val nodeList = readNodes(ref)
        return when {
            nodeList.length == 1 -> {
                nodeList.item(0).textContent as T
            }

            nodeList.length > 1 -> {
                val result = mutableListOf<String>()
                for (i in 0..<nodeList.length) {
                    result.add(nodeList.item(i).textContent)
                }
                result as T
            }

            else -> {
                null
            }
        }
    }

    override fun map(ref: String, mapper: (current: Any) -> Any) {
        val nodeList = readNodes(ref)
        for (i in 0..<nodeList.length) {
            val node = nodeList.item(i)
            node.textContent?.let { content ->
                node.textContent = mapper(content).toString()
            }
        }
    }

    override fun map(ref: String, mapper: (current: Any, ref: String, args: Array<String>) -> Any) {
        val nodeList = readNodes(ref)
        for (i in 0..<nodeList.length) {
            val node = nodeList.item(i)
            val path = computeXPath(node)
            node.textContent?.let { content ->
                node.textContent = mapper(content, path, extractArguments(ref, path)).toString()
            }
        }
    }

    private fun readNodes(path: String): NodeList {
        val xpath = xpath.get().also {
            it.reset()
        }
        return xpath.evaluate(path, document, XPathConstants.NODESET) as NodeList
    }

    @Suppress("ReturnCount")
    private fun computeXPath(node: Node): String {
        if (node.nodeType == Node.ATTRIBUTE_NODE) {
            return computeXPath((node as Attr).ownerElement) + "/@" + node.nodeName
        }
        if (node.nodeType == Node.TEXT_NODE) {
            return computeXPath(node.parentNode as Node) + "/text()"
        }
        node.parentNode?.let { parent ->
            val siblings = mutableListOf<Node>()
            for (i in 0..<parent.childNodes.length) {
                val sibling = parent.childNodes.item(i)
                if (sibling.nodeType == Node.ELEMENT_NODE && sibling.nodeName == node.nodeName) {
                    siblings.add(sibling)
                }
            }
            val index = siblings.indexOf(node) + 1
            return computeXPath(parent) + "/" + node.nodeName + "[$index]"
        }
        return ""
    }

    private fun extractArguments(indefiniteRef: String, definiteRef: String): Array<String> {
        val path = if (!indefiniteRef.startsWith("/")) {
            "/$indefiniteRef"
        } else {
            indefiniteRef
        }
        val isRootPath = !path.startsWith("//")

        val segments = path.split("/").mapIndexed { index, s ->
            if (index > 0 && s.isNotEmpty() && !s.startsWith('@')) {
                when {
                    s.matches("""\*""".toRegex()) -> {
                        """(.*)"""
                    }

                    s.matches(""".+\[\d+]$""".toRegex()) -> {
                        s
                    }

                    s.matches(""".+\[.+]$""".toRegex()) -> {
                        s.replace("""\[.+]$""".toRegex(), Regex.escapeReplacement("""\[(\d+)]"""))
                    }

                    isRootPath && index == 1 -> {
                        """$s\[\d+]"""
                    }

                    s == "text()" -> {
                        """text\(\)"""
                    }

                    else -> {
                        """$s\[(\d+)]"""
                    }
                }
            } else {
                s
            }
        }

        val regexStr = segments
            .joinToString("/")
            .replace("//", """/?(.*)/""")

        return regexStr.toRegex().matchEntire(definiteRef)?.groupValues?.drop(1)?.toTypedArray() ?: emptyArray()
    }
}
