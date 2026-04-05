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
package me.velikiy.frozenflow.transform.response.body.processor

import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.support.*

object ValueBodyTemplateProcessor : ResponseBodyTemplateProcessor {

    override fun process(body: ResponseBodyAccessor, bodyTemplate: Response.Template) {
        requireNotNull(bodyTemplate.value) {
            "Value must be specified in the value template for attribute: '${bodyTemplate.ref}'!"
        }

        body.map(bodyTemplate.ref) { _, _, args ->
            if (bodyTemplate.value is String) {
                bodyTemplate.value.replace(PLACEHOLDER_REGEX) { match ->
                    val indexStr = match.groupValues[1]
                    try {
                        val index = indexStr.toInt()
                        check(index in args.indices) {
                            "Argument does not exist for placeholder index: '{$index}' " +
                                "in the template value attribute: '${bodyTemplate.ref}'"
                        }
                        args[index]
                    } catch (_: NumberFormatException) {
                        error(
                            "Incorrect placeholder index format '{$indexStr}' " +
                                "in the template value attribute: '${bodyTemplate.ref}'"
                        )
                    }
                }
            } else {
                bodyTemplate.value
            }
        }
    }
}

private val PLACEHOLDER_REGEX = """\{(\d+)}""".toRegex()
