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
package me.velikiy.frozenflow.init

import com.fasterxml.jackson.core.type.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.yaml.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*
import me.velikiy.frozenflow.transform.model.*
import java.io.*

data class Config(
    val general: General = General(),
    val snapshot: Snapshot = Snapshot(),
    val services: Map<String, Service> = emptyMap(),
    val serviceConfigFiles: List<String> = emptyList(),
) {
    data class General(
        val maxMissReports: Int = 100,
    )

    data class Snapshot(
        val headers: Set<String> = emptySet(),
    )

    data class Service(
        val proxyBaseUrl: String,
        val transformers: Transformers = Transformers(),
        val mappings: List<Mapping> = emptyList(),
        val paths: List<Path> = emptyList(),
    ) {
        data class Mapping(
            val id: String? = null,
            val conditions: Conditions = Conditions(),
            val request: Request = Request(),
            val response: Response = Response(),
            val state: List<State> = emptyList(),
            val priority: Int? = null,
        ) {
            fun toModel(serviceId: String) = Mapping(
                id,
                serviceId,
                conditions,
                request,
                response,
                state,
                priority
            )
        }

        data class Path(
            val path: String,
            val transformers: Transformers = Transformers(),
            val mappings: List<Mapping> = emptyList(),
        ) {
            fun toModel(serviceId: String) = me.velikiy.frozenflow.transform.model.Service.Path(
                path,
                transformers.request,
                transformers.response,
                mappings.map { it.toModel(serviceId) }
            )
        }

        data class Transformers(
            val request: List<String> = emptyList(),
            val response: List<String> = emptyList(),
        )

        fun toModel(serviceId: String) = Service(
            proxyBaseUrl,
            transformers.request,
            transformers.response,
            mappings.map { it.toModel(serviceId) },
            paths.map { it.toModel(serviceId) }
        )
    }

    companion object {
        fun load(path: String): Config {
            val mapper = YAMLMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.KEBAB_CASE
                registerKotlinModule()
                registerModules(JavaTimeModule())
            }
            val config: Config = FileInputStream("$path/$SETTINGS_FILE").use {
                mapper.readValue(it, Config::class.java)
            }
            val services = config.serviceConfigFiles.flatMap { file ->
                FileInputStream("$path/$file").use {
                    mapper.readValue(it, object : TypeReference<Map<String, Service>>() {})
                }.entries
            }.associate { it.toPair() }
            return config.copy(services = config.services + services)
        }
    }
}
