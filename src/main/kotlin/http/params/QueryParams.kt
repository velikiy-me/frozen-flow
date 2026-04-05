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
package me.velikiy.frozenflow.http.params

import me.velikiy.frozenflow.http.utils.*

interface QueryParams : MultiStringMap {

    companion object {

        fun of(vararg pairs: Pair<String, String>) = of(pairs.asIterable())

        fun of(pairs: Iterable<Pair<String, String>>): QueryParams =
            pairs.fold(QueryParamsImpl()) { map, (name, value) ->
                map.apply { add(name, value) }
            }
    }
}

class QueryParamsImpl : QueryParams, MultiStringMapImpl(caseInsensitive = false)
