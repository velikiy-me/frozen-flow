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
package me.velikiy.frozenflow.transform.request

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.request.body.*
import me.velikiy.frozenflow.transform.request.headers.*
import me.velikiy.frozenflow.transform.request.params.*
import me.velikiy.frozenflow.transform.request.state.*

object CompositeRequestPatternTransformer : StubMappingTransformer {

    override fun transform(stub: StubMapping, mapping: Mapping): StubMapping {
        if (mapping.request.patterns.queryParams.isNotEmpty()) {
            QueryParamsRequestPatternTransformer.transform(stub, mapping)
        }
        if (mapping.request.patterns.headers.isNotEmpty()) {
            HeadersRequestPatternTransformer.transform(stub, mapping)
        }
        if (mapping.request.patterns.body.isNotEmpty()) {
            RequestBodyPatternTransformer.transform(stub, mapping)
        }
        if (mapping.request.patterns.state.isNotEmpty()) {
            StatePatternTransformer.transform(stub, mapping)
        }
        return stub
    }
}
