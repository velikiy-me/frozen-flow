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
package me.velikiy.frozenflow.snapshot

import com.github.tomakehurst.wiremock.admin.*
import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.common.url.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.init.*
import java.net.*

class SnapshotTask(
    private val serverInitializer: ServerInitializer,
) : AdminTask {

    override fun execute(
        admin: Admin,
        serveEvent: ServeEvent,
        pathParams: PathParams,
    ): ResponseDefinition {
        val spec = if (serveEvent.request.body.isNotEmpty()) {
            Json.read(serveEvent.request.bodyAsString, SnapshotSpec::class.java)
        } else {
            null
        }
        return ResponseDefinitionBuilder.jsonResponse(
            serverInitializer.snapshot(spec?.name),
            HttpURLConnection.HTTP_OK
        )
    }
}

private data class SnapshotSpec(
    val name: String? = null,
)
