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
package me.velikiy.frozenflow.transform.request.state

import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.StubMappingTransformer
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.support.wiremock.CustomMatcherDefinition

object StatePatternTransformer : StubMappingTransformer {

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        if (mapping.request.patterns.state.isEmpty()) return stub

        val matcherParams = mapping.request.patterns.state.map { pattern ->
            when (pattern.type) {
                "exists" -> "hasContext" to pattern.ref
                else -> throw IllegalArgumentException("Unsupported state pattern type: ${pattern.type}")
            }
        }

        return stub.apply {
            request = RequestPatternBuilder.like(stub.request).andMatching(
                CustomMatcherDefinition(
                    "state-matcher",
                    if (matcherParams.size == 1) {
                        Parameters.one(matcherParams[0].first, matcherParams[0].second)
                    } else {
                        Parameters.one("and", matcherParams.map { mapOf(it) })
                    }
                )
            ).build()
        }
    }
}
