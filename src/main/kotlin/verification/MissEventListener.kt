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
package me.velikiy.frozenflow.verification

import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.stubbing.*
import com.github.tomakehurst.wiremock.verification.diff.*
import me.velikiy.frozenflow.content.json.*
import me.velikiy.frozenflow.utils.*

class MissEventListener(
    private val missTracker: MissTracker,
) : ServeEventListener {

    override fun getName() = "miss-event-listener"

    override fun afterComplete(serveEvent: ServeEvent, parameters: Parameters) {
        serveEvent.subEvents.find { it.type == SubEvent.NON_MATCH_TYPE }?.let { nonMatchSubevent ->
            val httpCall = serveEvent.toHttpCall()
            missTracker.register(
                MissReport(
                    service = httpCall.serviceName,
                    uri = httpCall.request.uri,
                    method = httpCall.request.method,
                    message = Json.mapToObject<DiffEventData>(nonMatchSubevent.data).report,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }
}
