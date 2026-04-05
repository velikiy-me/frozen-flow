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
package me.velikiy.frozenflow.http.headers

import io.github.oshai.kotlinlogging.*
import java.nio.charset.*

private val CHARSET_REGEX = """charset=(.*)""".toRegex()

private val JSON_MIME_TYPE_PATTERNS: List<Regex> = listOf(
    ".*json.*"
).map { it.toRegex() }

private val XML_MIME_TYPE_PATTERNS: List<Regex> = listOf(
    ".*xml.*"
).map { it.toRegex() }

private val OTHER_TEXT_MIME_TYPE_PATTERNS: List<Regex> = listOf(
    ".*text.*",
    ".*html.*",
    ".*yaml.*",
    ".*csv.*",
    ".*x-www-form-urlencoded.*"
).map { it.toRegex() }

private val TEXT_MIME_TYPE_PATTERNS: List<Regex> =
    JSON_MIME_TYPE_PATTERNS + XML_MIME_TYPE_PATTERNS + OTHER_TEXT_MIME_TYPE_PATTERNS

private val logger = KotlinLogging.logger {}

class ContentType(value: String) : Header(value) {
    private val parts = value.split(";")

    val mimeType: String? = parts.firstOrNull()

    val charset: Charset? by lazy {
        parts.asSequence()
            .drop(1)
            .mapNotNull {
                CHARSET_REGEX.find(it)?.groupValues?.getOrNull(1)
            }
            .firstNotNullOfOrNull { charset ->
                try {
                    Charset.forName(charset)
                } catch (e: UnsupportedCharsetException) {
                    logger.warn(e) { "Unsupported charset in Content-Type header '$value': ${e.message}" }
                    null
                }
            }
    }

    val isTextContent: Boolean
        get() =
            mimeType?.let { mimeType -> TEXT_MIME_TYPE_PATTERNS.any { it.matches(mimeType) } } == true

    val isJson: Boolean
        get() =
            mimeType?.let { mimeType -> JSON_MIME_TYPE_PATTERNS.any { it.matches(mimeType) } } == true

    val isXml: Boolean
        get() =
            mimeType?.let { mimeType -> XML_MIME_TYPE_PATTERNS.any { it.matches(mimeType) } } == true
}
