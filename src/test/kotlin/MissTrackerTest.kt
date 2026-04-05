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
package me.velikiy.frozenflow

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import me.velikiy.frozenflow.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MissTrackerTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    @Test
    fun `test total misses and reports in playback mode`(): Unit = runBlocking {
        wiremockExtension.client.get("test-service/some-path")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val matchedResponse = wiremockExtension.client.get("test-service/some-path")

        assert(matchedResponse.status == HttpStatusCode.OK)
        with(getMissesInfo()) {
            assert(total == 0L)
            assert(misses.isEmpty())
        }

        val missedResponse1 = wiremockExtension.client.get("non-existent-service/some-path")

        assert(missedResponse1.status == HttpStatusCode.NotFound)
        with(getMissesInfo()) {
            assert(total == 1L)
            assert(misses.size == 1)
            with(misses[0]) {
                assert(service == "non-existent-service")
                assert(uri == "/non-existent-service/some-path")
            }
        }

        val missedResponse2 = wiremockExtension.client.get("test-service/non-existent-path")

        assert(missedResponse2.status == HttpStatusCode.NotFound)
        with(getMissesInfo()) {
            assert(total == 2L)
            assert(misses.size == 2)
            with(misses[0]) {
                assert(service == "test-service")
                assert(uri == "/test-service/non-existent-path")
            }
            with(misses[1]) {
                assert(service == "non-existent-service")
                assert(uri == "/non-existent-service/some-path")
            }
        }

        resetMissesCount()

        with(getMissesInfo()) {
            assert(total == 0L)
            assert(misses.isEmpty())
        }
    }

    @Test
    fun `test max miss reports configuration`(): Unit = runBlocking {
        wiremockExtension.client.get("test-service/some-path")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        repeat(3) { i ->
            wiremockExtension.client.get("test-service/other-path-$i")
        }

        with(getMissesInfo()) {
            assert(total == 3L)
            assert(misses.size == 2)
            assert(misses[0].uri == "/test-service/other-path-2")
            assert(misses[1].uri == "/test-service/other-path-1")
        }
    }

    private suspend fun getMissesInfo(): MissesResponse {
        val response = wiremockExtension.client.get("__admin/extended/misses")

        assert(response.status == HttpStatusCode.OK)

        return response.body<MissesResponse>()
    }

    private suspend fun resetMissesCount() {
        val response = wiremockExtension.client.post("__admin/extended/misses/reset")

        assert(response.status == HttpStatusCode.NoContent)
    }
}

@Serializable
private data class MissesResponse(
    val total: Long,
    val misses: List<MissReport>,
)

@Serializable
private data class MissReport(
    val service: String,
    val uri: String,
    val method: String,
    val message: String,
    val timestamp: Long,
)
