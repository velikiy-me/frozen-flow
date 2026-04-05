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
package me.velikiy.frozenflow.http.encoder

import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.headers.*
import me.velikiy.frozenflow.http.utils.*

private const val REQUEST_TOKEN = "-->"
private const val RESPONSE_TOKEN = "<--"
private const val METADATA_TOKEN = "---"
private const val NEW_LINE_PATTERN = """(\n|\r\n)"""
private const val WHITESPACE_PATTERN = """[\t ]"""

private val HTTP_CALL_REQUEST_RESPONSE_REGEX = "$NEW_LINE_PATTERN$RESPONSE_TOKEN$NEW_LINE_PATTERN".toRegex()
private val HTTP_CALL_METADATA_SPLIT_REGEX = "$NEW_LINE_PATTERN$METADATA_TOKEN$NEW_LINE_PATTERN".toRegex()
private val EMPTY_STRING_REGEX = """$NEW_LINE_PATTERN$WHITESPACE_PATTERN*$NEW_LINE_PATTERN""".toRegex()
private val LAST_EMPTY_STRING_REGEX = """$NEW_LINE_PATTERN$WHITESPACE_PATTERN*$""".toRegex()
private val WHITESPACE_REGEX = """$WHITESPACE_PATTERN+""".toRegex()
private val HEADER_DIVIDER_REGEX = """:$WHITESPACE_PATTERN+""".toRegex()

interface HttpCallEncoder {

    fun encode(call: HttpCall): String

    fun decode(encoded: String): HttpCall
}

interface HttpRequestEncoder {

    fun encode(request: Request): String

    fun decode(encoded: String): Request
}

interface HttpResponseEncoder {

    fun encode(response: Response): String

    fun decode(encoded: String): Response
}

@Suppress("TooManyFunctions")
class DefaultHttpCallEncoder(
    private val requestEncoder: HttpRequestEncoder,
    private val responseEncoder: HttpResponseEncoder,
) : HttpCallEncoder {

    override fun encode(call: HttpCall): String =
        buildString {
            appendLine(REQUEST_TOKEN)
            append(requestEncoder.encode(call.request))
            appendLine()
            appendLine(RESPONSE_TOKEN)
            append(responseEncoder.encode(call.response))
            if (call.metadata.isNotEmpty()) {
                appendLine()
                appendLine(METADATA_TOKEN)
                encodeMetadata(call.metadata)
            }
        }

    private fun StringBuilder.encodeMetadata(metadata: Map<String, String>) {
        metadata.forEach { (name, value) ->
            appendLine("$name: $value")
        }
    }

    @Suppress("MagicNumber")
    override fun decode(encoded: String): HttpCall {
        require(encoded.contains(REQUEST_TOKEN)) { "Request definition was not found!" }
        require(encoded.contains(RESPONSE_TOKEN)) { "Response definition was not found!" }

        val callMetadataParts = encoded.split(HTTP_CALL_METADATA_SPLIT_REGEX, limit = 2)
        val callRequestResponsePart = callMetadataParts[0]
        val metadataPart = callMetadataParts.getOrNull(1)

        val callParts = callRequestResponsePart.split(HTTP_CALL_REQUEST_RESPONSE_REGEX, limit = 2)

        require(callParts.count() == 2) {
            "Malformed http call definition!"
        }

        val request = callParts[0].removePrefix(REQUEST_TOKEN).trimStart().also {
            require(it.contains(LAST_EMPTY_STRING_REGEX)) {
                "Request definition must be followed by a new blank string!"
            }
        }.replace(LAST_EMPTY_STRING_REGEX, "")

        val response = callParts[1].trimStart().also {
            require(it.contains(LAST_EMPTY_STRING_REGEX)) {
                "Response definition must be followed by a new blank string!"
            }
        }.replace(LAST_EMPTY_STRING_REGEX, "")

        val metadata = metadataPart?.trimStart()?.also {
            require(it.contains(LAST_EMPTY_STRING_REGEX)) {
                "Metadata definition must be followed by a new blank string!"
            }
        }?.replace(LAST_EMPTY_STRING_REGEX, "")

        return HttpCall(
            request = requestEncoder.decode(request),
            response = responseEncoder.decode(response),
            metadata = metadata?.let { decodeMetadata(it).toMutableMap() } ?: mutableMapOf()
        )
    }

    private fun decodeMetadata(metadata: String): Map<String, String> =
        metadata.lineSequence().map { parseMetadataEntry(it) }.toMap()

    private fun parseMetadataEntry(entry: String): Pair<String, String> =
        entry.split(HEADER_DIVIDER_REGEX, 2).also {
            require(it.count() == 2) { "Malformed metadata entry definition: '$entry'!" }
        }.let { it[0] to it[1] }
}

object DefaultHttpRequestEncoder : HttpRequestEncoder {

    override fun encode(request: Request): String = buildString {
        appendLine("${request.method} ${request.uri}")
        encodeHeaders(request.headers)
        if (request.body != null) {
            appendLine()
            appendLine(request.body.asString())
        }
    }

    override fun decode(encoded: String): Request {
        val requestParts = encoded.split(EMPTY_STRING_REGEX, 2)

        val headLinesIterator = requestParts[0].lineSequence().iterator()

        val requestLine = headLinesIterator.next()
        val uriStringTokens = requestLine.split(WHITESPACE_REGEX).map { it.trim() }.also {
            require(it.count() == 2) {
                "Request line must contain exactly two parts: http method and uri! Actual value: '$requestLine'"
            }
        }

        val method = uriStringTokens[0]
        val uri = uriStringTokens[1]
        val headers = headLinesIterator.parseHeaders()
        val body = if (requestParts.count() == 2) bodyToBytes(requestParts[1], headers.contentType) else null

        return Request(method, uri, headers, body)
    }
}

object DefaultHttpResponseEncoder : HttpResponseEncoder {

    override fun encode(response: Response): String = buildString {
        appendLine(response.status)
        encodeHeaders(response.headers)
        if (response.body != null) {
            appendLine()
            appendLine(response.body.asString())
        }
    }

    override fun decode(encoded: String): Response {
        val responseParts = encoded.split(EMPTY_STRING_REGEX, 2)

        val headLinesIterator = responseParts[0].lineSequence().iterator()

        val statusLine = headLinesIterator.next()

        val status = statusLine.toIntOrNull() ?: error("Malformed response status line: '$statusLine'!")
        val headers = headLinesIterator.parseHeaders()
        val body = if (responseParts.count() == 2) bodyToBytes(responseParts[1], headers.contentType) else null

        return Response(status, headers, body)
    }
}

private fun StringBuilder.encodeHeaders(headers: HttpHeaders) {
    headers.forEach { (name, values) ->
        values.forEach { value ->
            appendLine("$name: $value")
        }
    }
}

private fun Iterator<String>.parseHeaders(): HttpHeaders =
    asSequence()
        .map { parseHeader(it) }
        .fold(HttpHeaders.of()) { headers, (name, value) ->
            headers.apply { add(name, value) }
        }

private fun parseHeader(header: String): Pair<String, String> =
    header.split(HEADER_DIVIDER_REGEX, 2).also {
        require(it.count() == 2) { "Malformed header definition: '$header'!" }
    }.let { it[0] to it[1] }
