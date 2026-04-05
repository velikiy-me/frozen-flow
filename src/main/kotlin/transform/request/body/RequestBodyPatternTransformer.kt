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
package me.velikiy.frozenflow.transform.request.body

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.request.body.processor.*
import me.velikiy.frozenflow.transform.request.body.processor.json.*
import me.velikiy.frozenflow.transform.request.body.processor.xml.*
import me.velikiy.frozenflow.transform.support.*
import me.velikiy.frozenflow.transform.support.json.*
import me.velikiy.frozenflow.transform.support.xml.*

object RequestBodyPatternTransformer : StubMappingTransformer {

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        RequestBodyAccessor.of(stub).use { body ->
            val processor: RequestBodyPatternProcessor = when (body) {
                is JsonRequestBodyAccessor -> CompositeJsonBodyPatternProcessor
                is XmlRequestBodyAccessor -> CompositeXmlBodyPatternProcessor
                else -> error("Unknown request body accessor type: '${body::class.simpleName}'!")
            }
            mapping.request.patterns.body.forEach { pattern ->
                processor.process(stub.request, body, pattern, stub.metadata)
            }
        }
        return stub
    }
}
