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
package me.velikiy.frozenflow.transform.request.common

import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.request.*

object MultiValuePatternFactoryImpl : MultiValuePatternFactory {

    override fun create(pattern: Request.Pattern): MultiValuePattern {
        require(pattern.value is String?) {
            "Expected string value representation for pattern value attribute: '${pattern.ref}'!"
        }
        return when (pattern.type) {
            "regex" -> MultiValuePattern.of(
                RegexPattern(
                    requireNotNull(pattern.value) {
                        "Pattern value attribute is required for regex type!"
                    }
                )
            )
            "uuid" -> MultiValuePattern.of(RegexPattern("^$UUID_REGEX$"))
            "value" -> MultiValuePattern.of(
                EqualToPattern(
                    requireNotNull(pattern.value) {
                        "Pattern value attribute is required for value type!"
                    }
                )
            )
            else -> error("Unknown pattern type: '${pattern.type}'!")
        }
    }
}
