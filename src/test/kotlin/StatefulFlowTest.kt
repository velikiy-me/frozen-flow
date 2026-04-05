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
import java.util.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatefulFlowTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    @Test
    fun `test stateful flow`() = runBlocking {
        val firstValue = "Value 1"
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$firstValue"}""")
        }
        val secondValue = "Value 2"
        wiremockExtension.client.put("test-stateful-flow/store/2") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$secondValue"}""")
        }
        wiremockExtension.client.get("test-stateful-flow/retrieve/1")
        wiremockExtension.client.get("test-stateful-flow/retrieve/2")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val firstRandomValue = UUID.randomUUID().toString()
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$firstRandomValue"}""")
        }
        val secondRandomValue = UUID.randomUUID().toString()
        wiremockExtension.client.put("test-stateful-flow/store/2") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$secondRandomValue"}""")
        }
        val firstResponse = wiremockExtension.client.get("test-stateful-flow/retrieve/1")
        val secondResponse = wiremockExtension.client.get("test-stateful-flow/retrieve/2")

        assert(firstResponse.status.value == 200)
        assert(firstResponse.body<ResponseValue>().value == firstRandomValue)
        assert(secondResponse.status.value == 200)
        assert(secondResponse.body<ResponseValue>().value == secondRandomValue)
    }

    @Test
    fun `test stateful flow with deletion`() = runBlocking {
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"Value 1"}""")
        }
        wiremockExtension.client.get("test-stateful-flow/retrieve/1")
        wiremockExtension.client.delete("test-stateful-flow/delete/1")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val randomValue = UUID.randomUUID().toString()
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$randomValue"}""")
        }
        val responseBefore = wiremockExtension.client.get("test-stateful-flow/retrieve/1")
        assert(responseBefore.status.value == 200)
        assert(responseBefore.body<ResponseValue>().value == randomValue)

        wiremockExtension.client.delete("test-stateful-flow/delete/1")

        val responseAfter = wiremockExtension.client.get("test-stateful-flow/retrieve/1")
        assert(responseAfter.status.value == 200)
        assert(responseAfter.body<ResponseValue>().value == "")
    }

    @Test
    fun `test stateful flow with state checking`() = runBlocking {
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"Value 1"}""")
        }
        wiremockExtension.client.get("test-stateful-flow/check/1")
        wiremockExtension.client.get("test-stateful-flow/check/2")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val randomValue = UUID.randomUUID().toString()
        wiremockExtension.client.put("test-stateful-flow/store/1") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"$randomValue"}""")
        }
        val responseExisting = wiremockExtension.client.get("test-stateful-flow/check/1")
        assert(responseExisting.status.value == 200)
        assert(responseExisting.body<ResponseValue>().value == randomValue)

        val responseAbsent = wiremockExtension.client.get("test-stateful-flow/check/2")
        assert(responseAbsent.status.value == 404)
    }

    @Test
    fun `test stub deduplication with state checking in place`() = runBlocking {
        wiremockExtension.client.get("test-stateful-flow/check")
        wiremockExtension.client.get("test-stateful-flow/check")

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assert(wiremockExtension.stubs.size == 1)
    }

    @Serializable
    private data class ResponseValue(val value: String)
}
