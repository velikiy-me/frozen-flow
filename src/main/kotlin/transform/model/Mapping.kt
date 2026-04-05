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
package me.velikiy.frozenflow.transform.model

import com.fasterxml.jackson.annotation.*

data class Mapping(
    val id: String? = null,
    val serviceId: String,
    val conditions: Conditions = Conditions(),
    val request: Request = Request(),
    val response: Response = Response(),
    val state: List<State> = emptyList(),
    val priority: Int? = null,
) {
    fun mergeWith(mapping: Mapping) = Mapping(
        id = id ?: mapping.id,
        serviceId = serviceId,
        conditions = conditions,
        request = Request(
            patterns = Request.Patterns(
                headers = request.patterns.headers + mapping.request.patterns.headers,
                body = request.patterns.body + mapping.request.patterns.body,
            ),
            dependencies = Request.Dependencies(
                body = request.dependencies.body + mapping.request.dependencies.body,
            )
        ),
        response = Response(
            mimeType = response.mimeType ?: mapping.response.mimeType,
            templates = Response.Templates(
                headers = response.templates.headers + mapping.response.templates.headers,
                body = response.templates.body + mapping.response.templates.body,
            )
        ),
        state = state + mapping.state,
        priority = priority ?: mapping.priority,
    )
}

data class Conditions(
    val headers: List<Condition> = emptyList(),
    val body: List<Condition> = emptyList(),
)

data class State(
    val action: Action,
    val context: String,
    val properties: Map<String, Any> = emptyMap(),
) {
    enum class Action {
        @JsonProperty("put")
        PUT,

        @JsonProperty("delete")
        DELETE,
    }
}
