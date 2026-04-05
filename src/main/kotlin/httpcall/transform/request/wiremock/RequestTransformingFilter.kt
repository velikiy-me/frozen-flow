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
package me.velikiy.frozenflow.httpcall.transform.request.wiremock

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.extension.requestfilter.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.Request
import me.velikiy.frozenflow.httpcall.transform.request.*
import me.velikiy.frozenflow.utils.*
import java.util.*
import com.github.tomakehurst.wiremock.http.Request as WmRequest

class RequestTransformingFilter(
    private val transformersConfig: Map<String, List<String>>,
) : StubRequestFilterV2 {

    override fun getName(): String = "request-transforming-filter"

    override fun filter(wmRequest: WmRequest, serveEvent: ServeEvent): RequestFilterAction {
        val transformers = transformersConfig[wmRequest.url]
        if (!transformers.isNullOrEmpty()) {
            val request = wmRequest.toRequest()
            val modifiedRequest =
                RequestTransformingService.applyTransformers(request, transformers)
            return RequestFilterAction.continueWith(wmRequest.mergeWith(modifiedRequest))
        }
        return RequestFilterAction.continueWith(wmRequest)
    }
}

private fun WmRequest.mergeWith(request: Request): WmRequest =
    WmRequestWrapper(this, request)

private class WmRequestWrapper(
    private val wmRequest: WmRequest,
    private val request: Request,
) : WmRequest by wmRequest {

    private val absoluteUrl = buildAbsoluteUrl()
    private val queryParams = Urls.splitQuery(request.uri)
    private val headers = HttpHeaders(
        request.headers.map { (name, values) ->
            HttpHeader(name, values)
        }
    )

    override fun getUrl(): String = request.uri

    override fun getAbsoluteUrl(): String = absoluteUrl

    override fun getMethod(): RequestMethod = RequestMethod.fromString(request.method)

    override fun getHeader(key: String): String? =
        headers.getHeader(key).let { if (it.isPresent) it.firstValue() else null }

    override fun header(key: String?): HttpHeader =
        headers.getHeader(key)

    override fun contentTypeHeader(): ContentTypeHeader =
        headers.contentTypeHeader

    override fun getHeaders(): HttpHeaders =
        headers

    override fun containsHeader(key: String): Boolean =
        headers.getHeader(key).isPresent

    override fun getAllHeaderKeys(): Set<String> =
        headers.keys()

    override fun queryParameter(key: String): QueryParameter? =
        queryParams[key]

    override fun getCookies(): Map<String, Cookie> =
        emptyMap()

    override fun getBody(): ByteArray =
        request.body?.asBytes() ?: ByteArray(0)

    override fun getBodyAsString(): String =
        request.body?.asString() ?: ""

    override fun getBodyAsBase64(): String =
        Base64.getEncoder().encodeToString(getBody())

    private fun buildAbsoluteUrl(): String {
        val portPart = if (port > -1) ":$port" else ""
        return "${wmRequest.scheme}://${wmRequest.host}${portPart}${request.uri}"
    }
}
