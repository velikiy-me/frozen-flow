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
package me.velikiy.frozenflow.http

import me.velikiy.frozenflow.http.headers.*
import me.velikiy.frozenflow.http.params.*
import me.velikiy.frozenflow.http.utils.*

data class HttpCall(
    val request: Request,
    val response: Response,
    val metadata: MutableMap<String, String> = mutableMapOf(),
)

@Suppress("FunctionNaming")
fun Request(method: String, uri: String, headers: HttpHeaders, body: ByteArray?) = Request(
    method = method,
    uri = uri,
    headers = headers,
    body = body?.let { Body(it, headers.contentType) }
)

@Suppress("FunctionNaming")
fun Request(method: String, uri: String, headers: HttpHeaders, body: String?) = Request(
    method = method,
    uri = uri,
    headers = headers,
    body = body?.let { Body(it, headers.contentType) }
)

data class Request(
    val method: String,
    val uri: String,
    val headers: HttpHeaders,
    val body: Body? = null,
) {
    fun getPath(): String = extractPath(uri)
    fun getQueryParams(): QueryParams = extractQueryParams(uri)
}

@Suppress("FunctionNaming")
fun Response(status: Int, headers: HttpHeaders, body: ByteArray?) = Response(
    status = status,
    headers = headers,
    body = body?.let { Body(it, headers.contentType) }
)

@Suppress("FunctionNaming")
fun Response(status: Int, headers: HttpHeaders, body: String?) = Response(
    status = status,
    headers = headers,
    body = body?.let { Body(it, headers.contentType) }
)

data class Response(
    val status: Int,
    val headers: HttpHeaders,
    val body: Body? = null,
)

@Suppress("FunctionNaming")
fun Body(content: String, contentType: ContentType? = null) = Body(
    bodyToBytes(content, contentType),
    contentType
)

data class Body(
    private val content: ByteArray,
    val contentType: ContentType? = null,
) {
    fun asString(): String = bodyToString(content, contentType)

    fun asBytes() = content

    fun copy(content: String, contentType: ContentType? = this.contentType): Body =
        Body(bodyToBytes(content, contentType), contentType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Body

        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        return result
    }
}
