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
package me.velikiy.frozenflow.transform.conditions.headers

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.conditions.*
import me.velikiy.frozenflow.transform.conditions.headers.checker.*
import me.velikiy.frozenflow.transform.model.*

object RequestHeadersConditionsChecker : ConditionsChecker {

    private val conditionChecker: RequestHeadersConditionChecker = CompositeHeadersConditionChecker

    override fun check(stub: StubMapping, mapping: Mapping): Boolean {
        return mapping.conditions.headers.all { condition ->
            conditionChecker.check(stub.request.headers, condition)
        }
    }
}
