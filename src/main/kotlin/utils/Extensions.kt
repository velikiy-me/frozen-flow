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
package me.velikiy.frozenflow.utils

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.content.*
import me.velikiy.frozenflow.content.json.Json
import me.velikiy.frozenflow.content.xml.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.Request
import me.velikiy.frozenflow.http.Response
import me.velikiy.frozenflow.http.headers.HttpHeaders
import java.time.*
import java.time.format.*
import java.util.*
import kotlin.io.path.*
import com.github.tomakehurst.wiremock.http.HttpHeaders as WmHttpHeaders
import com.github.tomakehurst.wiremock.http.Request as WmRequest

fun StubMapping.getServiceName(): String =
    getUrlParts()[0]

private fun StubMapping.getUrlParts() =
    splitPathToParts(request.urlPath)

var StubMapping.loggedAt: Instant
    get() {
        checkNotNull(metadata) {
            "Metadata is not initialized in stub mapping '$id'!"
        }
        return metadata.loggedAt
    }
    set(value) {
        if (metadata == null) {
            metadata = Metadata()
        }
        metadata.loggedAt = value
    }

val StubMapping.httpCallsOrEmpty: List<UUID>
    get() = metadata?.httpCalls ?: emptyList()

var Metadata.loggedAt: Instant
    get() = Instant.parse(getString("loggedAt"))
    set(value) {
        this["loggedAt"] = value.toString()
    }

var Metadata.httpCalls: List<UUID>
    get() = getList("httpCalls")?.map { UUID.fromString(it.toString()) } ?: emptyList()
    set(value) {
        this["httpCalls"] = value
    }

val HttpCall.serviceName: String
    get() = splitPathToParts(request.uri)[0]

val HttpCall.path: String
    get() = "/${splitPathToParts(request.getPath()).getOrNull(1) ?: ""}"

var HttpCall.id: UUID
    get() = checkNotNull(metadata["id"]?.let { UUID.fromString(it) }) {
        "Attribute 'id' is not initialized!"
    }
    set(value) {
        metadata["id"] = value.toString()
    }

var HttpCall.loggedAt: Instant
    get() = checkNotNull(metadata["loggedAt"]?.let { Instant.parse(it) }) {
        "Attribute 'loggedAt' is not initialized!"
    }
    set(value) {
        metadata["loggedAt"] = value.toString()
    }

private fun splitPathToParts(path: String) =
    path.substring(1).split('/', limit = 2)

fun FileSource.clear() =
    deleteDirectoryContents(Path(path))

fun Instant.toCompactFormat(): String =
    compactUtcFormatter.format(this)

private val compactUtcFormatter =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneId.of("UTC"))

fun WmRequest.toRequest(): Request {
    val headers = headers?.toHeaders() ?: HttpHeaders.of()
    return Request(
        method = method.name,
        uri = url,
        headers = headers,
        body = body?.takeIf { it.isNotEmpty() }?.let { tryToFormatBody(headers, it) }
    )
}

fun LoggedResponse.toResponse(): Response {
    val headers = headers?.toHeaders() ?: HttpHeaders.of()
    return Response(
        status = status,
        headers = headers,
        body = body
            ?.takeIf { it.isNotEmpty() }
            ?.let { decodeIfNecessary(headers, it) }
            ?.let { tryToFormatBody(headers, it) }
    )
}

fun ResponseDefinition.toResponse(): Response {
    val headers = headers?.toHeaders() ?: HttpHeaders.of()
    return Response(
        status = status,
        headers = headers,
        body = body
    )
}

fun ServeEvent.toHttpCall() = HttpCall(
    request.toRequest(),
    response.toResponse(),
)

private fun WmHttpHeaders.toHeaders(): HttpHeaders =
    HttpHeaders.of(
        all().flatMap { header ->
            header.values.map { value -> header.key to value }
        }
    )

private fun decodeIfNecessary(headers: HttpHeaders, body: ByteArray): ByteArray {
    val contentEncoding = headers.contentEncoding
    return when {
        contentEncoding == null -> body
        "gzip" in contentEncoding.value -> Gzip.unGzip(body)
        "br" in contentEncoding.value -> Brotli.decode(body)
        else -> body
    }
}

private fun tryToFormatBody(headers: HttpHeaders, body: ByteArray): ByteArray =
    headers.contentType?.let { contentType ->
        when {
            contentType.isJson -> Json
            contentType.isXml -> Xml
            else -> null
        }?.tryToParse(body)?.toByteArray(format = true)
    } ?: body
