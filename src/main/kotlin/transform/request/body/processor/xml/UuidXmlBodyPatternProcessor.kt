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
package me.velikiy.frozenflow.transform.request.body.processor.xml

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.request.*
import me.velikiy.frozenflow.transform.request.body.processor.*
import me.velikiy.frozenflow.transform.support.*

object UuidXmlBodyPatternProcessor : RequestBodyPatternProcessor {

    override fun process(
        request: RequestPattern,
        body: RequestBodyAccessor,
        requestBodyPattern: Request.Pattern,
        metadata: Metadata,
    ) {
        body.map(requestBodyPattern.ref) { currentValue, ref, _ ->
            require(currentValue is String) {
                "Expected string value representation for attribute: '$ref'!"
            }
            require("^$UUID_REGEX$".toRegex().matches(currentValue)) {
                "Attribute value: '$currentValue' does not match UUID pattern " +
                    "for attribute: '$ref'!"
            }

            request.bodyPatterns.add(
                MatchesXPathPattern(
                    ref,
                    RegexPattern("^$UUID_REGEX$")
                )
            )

            "\${xmlunit.ignore}"
        }
    }
}
