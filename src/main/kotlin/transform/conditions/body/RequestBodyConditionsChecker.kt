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
package me.velikiy.frozenflow.transform.conditions.body

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.transform.conditions.*
import me.velikiy.frozenflow.transform.conditions.body.checker.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.support.*

object RequestBodyConditionsChecker : ConditionsChecker {

    private val conditionChecker: RequestBodyConditionChecker = CompositeBodyConditionChecker

    override fun check(stub: StubMapping, mapping: Mapping): Boolean {
        RequestBodyAccessor.of(stub).use { body ->
            return mapping.conditions.body.all { condition ->
                conditionChecker.check(body, condition)
            }
        }
    }
}
