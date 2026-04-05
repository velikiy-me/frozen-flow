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

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.httpcall.transform.request.*
import me.velikiy.frozenflow.httpcall.transform.response.*
import me.velikiy.frozenflow.transform.conditions.*
import me.velikiy.frozenflow.transform.dependencies.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.request.*
import me.velikiy.frozenflow.transform.response.*
import me.velikiy.frozenflow.transform.state.*
import me.velikiy.frozenflow.utils.*

class HttpCallMappingProcessor(
    private val stubGenerator: StubGenerator,
    private val services: Map<String, Service>,
    private val conditionsChecker: ConditionsChecker = CompositeConditionsChecker,
    private val requestPatternTransformer: StubMappingTransformer = CompositeRequestPatternTransformer,
    private val responseDefinitionTransformer: StubMappingTransformer = CompositeResponseDefinitionTransformer,
    private val stateMappingTransformer: StubMappingTransformer = StateMappingTransformer,
) {
    fun process(calls: List<HttpCall>): List<StubMapping> {
        val dependenciesMappingTransformer: StubMappingTransformer = DependenciesMappingTransformer()
        return calls.sortedByDescending { it.loggedAt }.map { httpCall ->
            processHttpCall(dependenciesMappingTransformer, httpCall)
        }.groupBy { stub ->
            stub.request
        }.map { (_, group) ->
            group.first().apply {
                metadata.httpCalls = group.map { it.id }
                name = id.toString()
            }
        }
    }

    private fun processHttpCall(
        dependenciesMappingTransformer: StubMappingTransformer,
        httpCall: HttpCall,
    ): StubMapping {
        val service = services[httpCall.serviceName]
        val path = service?.paths?.find { it.path.toRegex().matches(httpCall.path) }

        val transformedCall = path?.let { p ->
            httpCall
                .applyRequestTransformers(service.requestTransformers + p.requestTransformers)
                .applyResponseTransformers(service.responseTransformers + p.responseTransformers)
        } ?: httpCall

        val stub = stubGenerator.generateStub(transformedCall).apply {
            isPersistent = true
            id = httpCall.id
            loggedAt = httpCall.loggedAt
        }

        val serviceMapping = service?.mappings?.find { conditionsChecker.check(stub, it) }
        val pathMapping = path?.mappings?.find { conditionsChecker.check(stub, it) }
        val resultingMapping = when {
            serviceMapping == null -> pathMapping
            pathMapping == null -> serviceMapping
            else -> pathMapping.mergeWith(serviceMapping)
        }
        return resultingMapping?.let { mapping ->
            requestPatternTransformer.transform(stub, mapping).let { stub ->
                responseDefinitionTransformer.transform(stub, mapping)
            }.apply {
                mapping.priority?.let {
                    priority = it
                }
            }.let { stub ->
                dependenciesMappingTransformer.transform(stub, mapping)
            }.let { stub ->
                stateMappingTransformer.transform(stub, mapping)
            }
        } ?: stub
    }

    private fun HttpCall.applyRequestTransformers(transformers: List<String>) =
        if (transformers.isNotEmpty()) {
            copy(
                request = RequestTransformingService.applyTransformers(
                    request = request,
                    transformers = transformers
                )
            )
        } else {
            this
        }

    private fun HttpCall.applyResponseTransformers(transformers: List<String>) =
        if (transformers.isNotEmpty()) {
            copy(
                response = ResponseTransformingService.applyTransformersForMapping(
                    response = response,
                    transformers = transformers
                )
            )
        } else {
            this
        }
}
