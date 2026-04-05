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
package me.velikiy.frozenflow.transform

import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.headers.*
import me.velikiy.frozenflow.http.params.*
import me.velikiy.frozenflow.transform.support.wiremock.EqualToJsonPattern
import me.velikiy.frozenflow.transform.support.wiremock.EqualToXmlPattern

private val EXCLUDED_RESPONSE_HEADERS = listOf(
    HttpHeaders.CONTENT_ENCODING.lowercase(),
    HttpHeaders.CONTENT_LENGTH.lowercase(),
    HttpHeaders.TRANSFER_ENCODING.lowercase()
)

class StubGenerator(includedHeaders: Set<String>) {

    private val includedHeaders = includedHeaders.map { it.lowercase() }

    fun generateStub(call: HttpCall): StubMapping =
        WireMock.request(call.request.method, WireMock.urlPathEqualTo(call.request.getPath())).run {
            generateQueryParamsPatterns(call.request.getQueryParams())
            generateHeadersPatterns(call.request.headers)
            generateBodyPattern(call.request)
            this
        }.willReturn(
            WireMock.status(call.response.status).run {
                call.response.headers
                    .filterKeys { it.lowercase() !in EXCLUDED_RESPONSE_HEADERS }
                    .forEach { (name, values) -> withHeader(name, *values.toTypedArray()) }
                call.response.body?.let { body ->
                    if (body.contentType?.isTextContent == true) {
                        withBody(body.asString())
                    } else {
                        withBody(body.asBytes())
                    }
                }
            }
        ).build()

    private fun MappingBuilder.generateQueryParamsPatterns(queryParams: QueryParams) {
        queryParams.forEach { (name, values) ->
            if (values.count() == 1) {
                withQueryParam(
                    name,
                    WireMock.equalToIgnoreCase(values.single())
                )
            } else {
                withQueryParam(
                    name,
                    WireMock.havingExactly(*values.map { WireMock.equalTo(it) }.toTypedArray())
                )
            }
        }
    }

    private fun MappingBuilder.generateHeadersPatterns(headers: HttpHeaders) {
        headers.forEach { (name, values) ->
            if (name.lowercase() in includedHeaders) {
                if (values.count() == 1) {
                    withHeader(
                        name,
                        WireMock.equalToIgnoreCase(values.single())
                    )
                } else {
                    withHeader(
                        name,
                        WireMock.havingExactly(*values.map { WireMock.equalToIgnoreCase(it) }.toTypedArray())
                    )
                }
            }
        }
    }

    private fun MappingBuilder.generateBodyPattern(request: Request) {
        request.body?.takeIf { it.asBytes().isNotEmpty() }?.let { body ->
            val mimeType = body.contentType?.mimeType
            withRequestBody(
                when {
                    mimeType == null -> WireMock.equalToIgnoreCase(body.asString())
                    "json" in mimeType -> EqualToJsonPattern(body.asString())
                    "xml" in mimeType -> EqualToXmlPattern(body.asString())
                    "multipart/form-data" == mimeType -> AnythingPattern()
                    !ContentTypes.determineIsTextFromMimeType(mimeType) -> WireMock.binaryEqualTo(body.asBytes())
                    else -> WireMock.equalToIgnoreCase(body.asString())
                }
            )
        }
    }
}
