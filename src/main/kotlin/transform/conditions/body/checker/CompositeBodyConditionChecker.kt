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
package me.velikiy.frozenflow.transform.conditions.body.checker

import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.support.*

object CompositeBodyConditionChecker : RequestBodyConditionChecker {

    override fun check(body: RequestBodyAccessor, condition: Condition): Boolean {
        val conditionChecker: RequestBodyConditionChecker = when (condition.type) {
            "exists" -> ExistsBodyConditionChecker
            "regex" -> RegexBodyConditionChecker
            "value" -> ValueBodyConditionChecker
            else -> error("Unknown condition type: '${condition.type}'!")
        }
        return conditionChecker.check(body, condition)
    }
}
