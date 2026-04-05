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
package me.velikiy.frozenflow.test

import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.content.json.*
import me.velikiy.frozenflow.transform.support.wiremock.EqualToJsonPattern
import me.velikiy.frozenflow.transform.support.wiremock.EqualToXmlPattern
import java.net.*
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern as WmEqualToJsonPattern
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern as WmEqualToXmlPattern

fun loadContentPatternsFromClassPath(
    path: String,
    vararg placeHolders: Pair<String, String>,
): List<ContentPattern<*>> =
    Json.read<List<ContentPattern<*>>>(resolvePlaceHolders(path, *placeHolders)).map { pattern ->
        when (pattern) {
            is WmEqualToJsonPattern -> EqualToJsonPattern(pattern.value)
            is WmEqualToXmlPattern -> EqualToXmlPattern(pattern.value)
            else -> pattern
        }
    }

fun loadStringFromClassPath(
    path: String,
    vararg placeHolders: Pair<String, String>,
): String {
    return resolvePlaceHolders(path, *placeHolders)
}

private fun resolvePlaceHolders(
    path: String,
    vararg placeHolders: Pair<String, String>,
): String {
    val inputStream = checkNotNull({}.javaClass.getResourceAsStream(path)) {
        "File not found $path !"
    }
    val str = String(inputStream.readAllBytes())
    return placeHolders.fold(str) { acc, pair ->
        acc.replace(Regex(pair.first), pair.second)
    }
}

fun computeRandomPort(): Int {
    val socket = ServerSocket(0)
    val port = socket.localPort
    socket.close()
    return port
}

fun buildPathToTestResources(testClass: Class<*>): String {
    val filePathParts = testClass.name.split(".").toMutableList()
    filePathParts[filePathParts.lastIndex] = filePathParts
        .last()
        .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
        .lowercase()
    return filePathParts.joinToString("/", "/")
}
