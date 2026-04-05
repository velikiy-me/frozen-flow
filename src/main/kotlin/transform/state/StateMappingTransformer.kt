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
package me.velikiy.frozenflow.transform.state

import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.StubMappingTransformer
import me.velikiy.frozenflow.transform.model.*

object StateMappingTransformer : StubMappingTransformer {

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        applyRequestState(stub, mapping)
        return stub
    }

    private fun applyRequestState(stub: StubMapping, mapping: Mapping) {
        val reducedState = mapping.state.groupingBy { it.action to it.context }.reduce { _, accumulator, element ->
            accumulator.copy(properties = accumulator.properties + element.properties)
        }.values
        val listeners = reducedState.map { state ->
            val listenerName = when (state.action) {
                State.Action.PUT -> "recordState"
                State.Action.DELETE -> "deleteState"
            }
            ServeEventListenerDefinition(
                listenerName,
                Parameters.from(
                    buildMap {
                        put("context", state.context)
                        if (state.action == State.Action.PUT) {
                            put("state", state.properties)
                        }
                    }
                )
            )
        }
        stub.setServeEventListenerDefinitions(
            buildList {
                stub.serveEventListeners?.let { addAll(it) }
                addAll(listeners)
            }
        )
    }
}
