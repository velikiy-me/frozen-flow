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
package me.velikiy.frozenflow.httpcall.transform.response.wiremock

import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.http.Body
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.Response
import me.velikiy.frozenflow.httpcall.transform.response.*
import me.velikiy.frozenflow.utils.*

class ResponseDefinitionTransformer(
    private val transformersConfig: Map<String, List<String>>,
) : ResponseDefinitionTransformerV2 {

    override fun getName(): String = "response-definition-transformer"

    override fun transform(serveEvent: ServeEvent): ResponseDefinition {
        val transformers = transformersConfig[serveEvent.request.url]
        return if (!transformers.isNullOrEmpty()) {
            val response = serveEvent.responseDefinition.toResponse()
            val modifiedResponse = ResponseTransformingService.applyTransformers(response, transformers)
            serveEvent.responseDefinition.mergeWith(modifiedResponse)
        } else {
            serveEvent.responseDefinition
        }
    }

    private fun ResponseDefinition.mergeWith(response: Response) =
        ResponseDefinitionBuilder
            .like(this)
            .withStatus(response.status)
            .withHeaders(HttpHeaders(response.headers.map { (name, values) -> HttpHeader(name, values) }))
            .run {
                if (response.body != null) {
                    if (response.body.contentType?.isTextContent == true) {
                        withBody(response.body.asString())
                    } else {
                        withBody(response.body.asBytes())
                    }
                } else {
                    withResponseBody(Body.none())
                }
            }
            .build()
}
