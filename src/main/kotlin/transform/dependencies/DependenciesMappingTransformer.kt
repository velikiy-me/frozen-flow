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
package me.velikiy.frozenflow.transform.dependencies

import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.support.*

class DependenciesMappingTransformer : StubMappingTransformer {

    private val processedStubs = mutableMapOf<RequestPattern, StubMapping>()
    private val storedDependencies =
        mutableMapOf<MappingKey, MutableMap<Request.Dependency, MutableList<StubMapping>>>()

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        registerDependentStub(mapping, stub)
        return processDuplicateStub(mapping, stub)
    }

    private fun registerDependentStub(
        mapping: Mapping,
        stub: StubMapping,
    ) {
        mapping.request.dependencies.body.forEach { bodyDependency ->
            storedDependencies.computeIfAbsent(
                MappingKey(
                    bodyDependency.service.takeUnless { it == "self" } ?: mapping.serviceId,
                    bodyDependency.mappingId
                )
            ) {
                mutableMapOf()
            }.computeIfAbsent(bodyDependency) {
                mutableListOf()
            }.add(stub)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun processDuplicateStub(
        mapping: Mapping,
        stub: StubMapping,
    ): StubMapping {
        val existingStub = processedStubs.putIfAbsent(stub.request, stub)

        if (existingStub != null && mapping.id != null) {
            val depsMap = storedDependencies[MappingKey(mapping.serviceId, mapping.id)]
            if (depsMap != null) {
                for ((dep, stubs) in depsMap) {
                    val currentStubValue = ResponseBodyAccessor.of(stub).read<Any>(dep.sourceRef)
                    val existingStubValue =
                        ResponseBodyAccessor.of(existingStub).read<Any>(dep.sourceRef)
                    val (currentStubValues, existingStubValues) = if (currentStubValue is List<*>) {
                        Pair(currentStubValue, existingStubValue as List<*>)
                    } else {
                        Pair(listOf(currentStubValue), listOf(existingStubValue))
                    }
                    stubs.forEach { dependentStub ->
                        RequestBodyAccessor.of(dependentStub).use {
                            for (i in currentStubValues.indices) {
                                val value = currentStubValues[i]
                                val existingValue = existingStubValues[i]!!
                                it.map(dep.ref) { currentDependentValue ->
                                    if (currentDependentValue == value) {
                                        existingValue
                                    } else {
                                        currentDependentValue
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return stub
    }

    private data class MappingKey(
        val service: String,
        val mappingId: String,
    )
}
