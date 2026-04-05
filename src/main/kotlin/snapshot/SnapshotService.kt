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

import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.recording.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.utils.*
import java.time.*

class SnapshotService(
    private val admin: Admin,
    private val httpCallRepository: HttpCallRepository,
) {
    fun takeSnapshot(name: String? = null): SnapshotRecordResult {
        val httpCalls = admin.serveEvents.serveEvents.map(ServeEvent::toHttpCall)
        httpCallRepository.saveAll(httpCalls, name?.takeIf { it.isNotBlank() } ?: generateSnapshotName())
        return SnapshotRecordResult.ids(httpCalls.map { it.id })
    }
}

private fun generateSnapshotName() = Instant.now().toCompactFormat()

private fun ServeEvent.toHttpCall(): HttpCall =
    HttpCall(
        request = request.toRequest(),
        response = response.toResponse(),
    ).apply {
        id = this@toHttpCall.id
        loggedAt = this@toHttpCall.request.loggedDate.toInstant()
    }
