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
package me.velikiy.frozenflow.transform.support.json

import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.content.json.*
import me.velikiy.frozenflow.transform.support.*
import me.velikiy.frozenflow.transform.support.wiremock.*

class JsonRequestBodyAccessor(private val stub: StubMapping) : RequestBodyAccessor {

    private val pattern = stub.request.bodyPatterns[0] as EqualToJsonPattern
    private val content = Json.parse(pattern.value)

    private var changed = false

    override fun exists(ref: String): Boolean = content.exists(ref)

    override fun <T> read(ref: String): T? = content.read(ref)

    override fun map(ref: String, mapper: (current: Any) -> Any) =
        content.map(ref) { current ->
            mapper(current).also {
                changed = true
            }
        }

    override fun map(ref: String, mapper: (current: Any, ref: String, args: Array<String>) -> Any) =
        content.map(ref) { current, r, args ->
            mapper(current, r, args).also {
                changed = true
            }
        }

    override fun close() {
        if (changed) {
            stub.request.bodyPatterns[0] = EqualToJsonPattern(content.toString())
            changed = false
        }
    }
}
