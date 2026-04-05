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
package me.velikiy.frozenflow.transform.response.body

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.response.body.processor.*
import me.velikiy.frozenflow.transform.support.*

object ResponseBodyDefinitionTransformer : StubMappingTransformer {

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        if (mapping.response.templates.body.isNotEmpty()) {
            ResponseBodyAccessor.of(stub, mapping.response.mimeType).use { body ->
                mapping.response.templates.body.forEach { template ->
                    CompositeBodyTemplateProcessor.process(body, template)
                }
            }
        }
        return stub
    }
}
