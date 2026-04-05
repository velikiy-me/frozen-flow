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

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.jayway.jsonpath.*
import com.jayway.jsonpath.spi.json.*
import com.jayway.jsonpath.spi.mapper.*
import me.velikiy.frozenflow.content.*

class JsonContentAccessor(
    node: JsonNode,
    private val objectMapper: ObjectMapper,
) : ContentAccessor {

    private val valueContext = JsonPath.parse(
        node,
        Configuration.defaultConfiguration()
            .jsonProvider(JacksonJsonNodeJsonProvider(objectMapper))
            .mappingProvider(JacksonMappingProvider(objectMapper))
            .addOptions(
                Option.SUPPRESS_EXCEPTIONS,
                Option.ALWAYS_RETURN_LIST
            )
    )

    private val pathContext by lazy {
        JsonPath.parse(
            node,
            Configuration.defaultConfiguration()
                .jsonProvider(JacksonJsonNodeJsonProvider(objectMapper))
                .mappingProvider(JacksonMappingProvider(objectMapper))
                .addOptions(
                    Option.SUPPRESS_EXCEPTIONS,
                    Option.AS_PATH_LIST
                )
        )
    }

    override fun exists(ref: String): Boolean {
        val result: List<*> = valueContext.readTyped(ref)
        return result.isNotEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> read(ref: String): T? {
        val result: List<*> = valueContext.readTyped(ref)
        return when {
            result.size == 1 -> result.single() as T
            result.size > 1 -> result as T
            else -> null
        }
    }

    override fun map(ref: String, mapper: (current: Any) -> Any) {
        valueContext.map(JsonPath.compile(ref)) { currentValue, _ ->
            treeToValue(currentValue)?.let { mapper(it) }
        }
    }

    override fun map(ref: String, mapper: (current: Any, ref: String, args: Array<String>) -> Any) {
        val jsonPath = JsonPath.compile(ref)
        if (jsonPath.isDefinite) {
            valueContext.map(jsonPath) { currentValue, _ ->
                treeToValue(currentValue)?.let { mapper(it, jsonPath.path, emptyArray()) }
            }
        } else {
            pathContext.readTyped<List<String>>(jsonPath).forEach { path ->
                read<Any?>(path)?.let { value ->
                    mapper(value, path, extractArguments(jsonPath.path, path)).also {
                        valueContext.set(path, it)
                    }
                }
            }
        }
    }

    private fun treeToValue(node: Any): Any? =
        objectMapper.treeToValue((node as TreeNode), Any::class.java)

    private inline fun <reified T> DocumentContext.readTyped(ref: String): T =
        read(ref, object : TypeRef<T>() {})

    private inline fun <reified T> DocumentContext.readTyped(ref: JsonPath): T =
        read(ref, object : TypeRef<T>() {})
}

private fun extractArguments(indefiniteRef: String, definiteRef: String): Array<String> {
    val regex = indefiniteRef
        .replace("[", "\\[")
        .replace("$", "\\$")
        .replace("\\[(?:\\*|-?\\d*[,:]\\d*|\\?(?:,\\?)*)]".toRegex(), Regex.escapeReplacement("['?([A-Za-z\\d]+?)'?]"))
        .replace("..", "((?:\\['?[A-Za-z\\d]+'?\\])*?)")
        .toRegex()

    return regex.matchEntire(definiteRef)?.groupValues?.drop(1)?.toTypedArray() ?: emptyArray()
}
