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
package me.velikiy.frozenflow.content.json

import com.fasterxml.jackson.databind.*
import me.velikiy.frozenflow.content.*
import java.nio.charset.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

class Json private constructor(
    val node: JsonNode,
) : Content, ContentAccessor by JsonContentAccessor(node, objectMapper) {

    private val normalizedNode: JsonNode by lazy {
        objectMapper.normalize(node.deepCopy())
    }

    override fun toString(
        charset: Charset,
        format: Boolean,
        normalize: Boolean,
    ): String =
        if (charset == Charsets.UTF_8) {
            getObjectWriter(format).writeValueAsString(getJsonNode(normalize))
        } else {
            toByteArray(format, normalize).toString(charset)
        }

    override fun toString() =
        toString(Charsets.UTF_8, format = false, normalize = false)

    override fun toByteArray(
        format: Boolean,
        normalize: Boolean,
    ): ByteArray =
        getObjectWriter(format).writeValueAsBytes(getJsonNode(normalize))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Json

        return node == other.node
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    private fun getJsonNode(normalize: Boolean): JsonNode =
        if (normalize) normalizedNode else node

    private fun getObjectWriter(format: Boolean): ObjectWriter =
        if (format) {
            objectMapper.writerWithDefaultPrettyPrinter()
        } else {
            objectMapper.writer()
        }

    companion object : ContentFactory<Json> {

        private val objectMapper = ObjectMapper()

        override fun parse(content: String, charset: Charset) =
            if (charset == Charsets.UTF_8) {
                Json(objectMapper.readTree(content))
            } else {
                parse(content.toByteArray(charset))
            }

        override fun parse(content: ByteArray) =
            Json(objectMapper.readTree(content))

        fun <T> read(json: String, type: KType): T =
            objectMapper.readValue(json, objectMapper.typeFactory.constructType(type.javaType))

        inline fun <reified T> read(json: String): T =
            read(json, typeOf<T>())

        fun <T> mapToObject(map: Map<String, Any>, type: KType): T =
            objectMapper.convertValue(map, objectMapper.typeFactory.constructType(type.javaType))

        inline fun <reified T> mapToObject(map: Map<String, Any>): T =
            mapToObject(map, typeOf<T>())
    }
}

private fun ObjectMapper.normalize(node: JsonNode): JsonNode =
    when {
        node.isObject -> {
            val sortedObject = createObjectNode()
            node.fields().asSequence()
                .sortedBy { it.key }
                .forEach { (key, value) ->
                    sortedObject.set<JsonNode>(key, normalize(value))
                }
            sortedObject
        }

        node.isArray -> {
            val sortedArray = createArrayNode()
            node.forEach { element ->
                sortedArray.add(normalize(element))
            }
            sortedArray
        }

        else -> node
    }
