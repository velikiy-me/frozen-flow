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
package me.velikiy.frozenflow.transform.support.xml

import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.content.xml.*
import me.velikiy.frozenflow.transform.support.*

class XmlResponseBodyAccessor(private val stub: StubMapping) : ResponseBodyAccessor {

    private val content = Xml.parse(stub.response.body)

    private var changed = false

    override fun exists(ref: String): Boolean = content.exists(ref)

    override fun <T> read(ref: String): T? = content.read(ref)

    override fun map(ref: String, mapper: (current: Any) -> Any) =
        content.map(ref) { current ->
            mapper(current).also {
                changed = true
            }
        }

    override fun map(ref: String, mapper: (current: Any, ref: String, args: Array<String>) -> Any) =
        content.map(ref) { current, r, args ->
            mapper(current, r, args).also {
                changed = true
            }
        }

    override fun close() {
        if (changed) {
            stub.response = with(stub.response) {
                ResponseDefinition(
                    status,
                    statusMessage,
                    Body(content.toString()),
                    bodyFileName,
                    headers,
                    additionalProxyRequestHeaders,
                    removeProxyRequestHeaders,
                    fixedDelayMilliseconds,
                    delayDistribution,
                    chunkedDribbleDelay,
                    proxyBaseUrl,
                    proxyUrlPrefixToRemove,
                    fault,
                    transformers,
                    transformerParameters,
                    wasConfigured()
                )
            }
            changed = false
        }
    }
}
