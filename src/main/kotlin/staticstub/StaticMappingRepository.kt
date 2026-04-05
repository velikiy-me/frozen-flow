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
package me.velikiy.frozenflow.staticstub

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.stubbing.*
import java.nio.file.*
import kotlin.io.path.*

class StaticMappingRepository(path: Path) {

    private val stubMappings: List<StubMapping> = if (path.exists() && path.isDirectory()) {
        Files.walk(path).use { paths ->
            paths.filter { path ->
                path.isRegularFile()
            }.map {
                Json.read(it.readText(Charsets.UTF_8), StubMapping::class.java)
            }.toList()
        }
    } else {
        emptyList()
    }

    fun getAll(): List<StubMapping> {
        return stubMappings
    }
}
