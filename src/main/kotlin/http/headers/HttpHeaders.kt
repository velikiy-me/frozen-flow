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
package me.velikiy.frozenflow.http.headers

import me.velikiy.frozenflow.http.utils.*

interface HttpHeaders : MultiStringMap {

    val contentType: ContentType? get() = this[CONTENT_TYPE]?.single()?.let { ContentType(it) }
    val contentEncoding: ContentEncoding? get() = this[CONTENT_ENCODING]?.single()?.let { ContentEncoding(it) }

    companion object {
        const val CONTENT_TYPE = "Content-Type"
        const val CONTENT_ENCODING = "Content-Encoding"
        const val CONTENT_LENGTH = "Content-Length"
        const val TRANSFER_ENCODING = "Transfer-Encoding"

        fun of(vararg pairs: Pair<String, String>) = of(pairs.asIterable())

        fun of(pairs: Iterable<Pair<String, String>>): HttpHeaders =
            pairs.fold(HttpHeadersImpl()) { map, (name, value) ->
                map.apply { add(name, value) }
            }
    }
}

class HttpHeadersImpl : HttpHeaders, MultiStringMapImpl(caseInsensitive = true)
