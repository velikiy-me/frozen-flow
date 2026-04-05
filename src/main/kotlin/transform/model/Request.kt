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

import java.time.*

data class Request(
    val patterns: Patterns = Patterns(),
    val dependencies: Dependencies = Dependencies(),
) {
    data class Patterns(
        val queryParams: List<Pattern> = emptyList(),
        val headers: List<Pattern> = emptyList(),
        val body: List<Pattern> = emptyList(),
        val state: List<Pattern> = emptyList(),
    )

    data class Pattern(
        val ref: String,
        val type: String = "value",
        val format: String? = null,
        val timezone: ZoneId? = null,
        val truncation: DateTimeTruncation? = null,
        val value: Any? = null,
    )

    data class Dependencies(
        val body: List<Dependency> = emptyList(),
    )

    data class Dependency(
        val ref: String,
        val service: String = "self",
        val mappingId: String,
        val sourceRef: String,
    )
}
