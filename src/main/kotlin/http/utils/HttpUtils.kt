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
package me.velikiy.frozenflow.http.utils

import me.velikiy.frozenflow.http.headers.*
import me.velikiy.frozenflow.http.params.*
import java.net.*
import java.util.*

fun bodyToString(body: ByteArray, contentType: ContentType? = null): String =
    if (contentType != null && contentType.isTextContent) {
        body.toString(contentType.charset ?: Charsets.UTF_8)
    } else {
        Base64.getEncoder().encodeToString(body)
    }

fun bodyToBytes(body: String, contentType: ContentType? = null): ByteArray =
    if (contentType != null && contentType.isTextContent) {
        body.toByteArray(contentType.charset ?: Charsets.UTF_8)
    } else {
        Base64.getDecoder().decode(body)
    }

fun extractPath(uri: String) = uri.split("?")[0]

fun extractQueryParams(uri: String) = uri.split("?").getOrNull(1)?.let { paramsString ->
    parseQueryParams(paramsString)
} ?: QueryParams.of()

fun parseQueryParams(paramsString: String): QueryParams =
    paramsString.split("&")
        .map { paramEntry ->
            val parts = paramEntry.split("=", limit = 2)
            val paramValue = parts.getOrNull(1)?.let { URLDecoder.decode(it, Charsets.UTF_8) }
            parts[0] to (paramValue ?: "")
        }
        .let { QueryParams.of(it) }
