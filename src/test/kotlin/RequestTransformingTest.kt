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

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import me.velikiy.frozenflow.test.*
import me.velikiy.frozenflow.transform.support.wiremock.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestTransformingTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    @Test
    fun `test json to string request transformer`(): Unit = runBlocking {
        val initialResponse = wiremockExtension.client.post("request-transform/json-string") {
            contentType(ContentType.Application.Json)
            setBody(""""{\"attribute\":\"value\"}"""")
        }

        assert(initialResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.stubs.single().request.bodyPatterns.single())
            .isEqualTo(
                EqualToJsonPattern(
                    """{"attribute":"modified value"}"""
                )
            )
        assert(wiremockExtension.stubs.single().response.body == """{"attribute":"modified value"}""")

        val stubResponse = wiremockExtension.client.post("request-transform/json-string") {
            contentType(ContentType.Application.Json)
            setBody(""""{\"attribute\":\"modified value\"}"""")
        }

        assert(stubResponse.status == HttpStatusCode.OK)
        assert(stubResponse.bodyAsText() == """"{\"attribute\":\"modified value\"}"""")
    }

    @Test
    fun `test json to string request transformer on root path`(): Unit = runBlocking {
        val initialResponse = wiremockExtension.client.post("request-transform") {
            contentType(ContentType.Application.Json)
            setBody(""""{\"attribute\":\"value\"}"""")
        }

        assert(initialResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.stubs.single().request.bodyPatterns.single())
            .isEqualTo(
                EqualToJsonPattern(
                    """{"attribute":"modified value"}"""
                )
            )
        assert(wiremockExtension.stubs.single().response.body == """{"attribute":"modified value"}""")

        val stubResponse = wiremockExtension.client.post("request-transform") {
            contentType(ContentType.Application.Json)
            setBody(""""{\"attribute\":\"modified value\"}"""")
        }

        assert(stubResponse.status == HttpStatusCode.OK)
        assert(stubResponse.bodyAsText() == """"{\"attribute\":\"modified value\"}"""")
    }

    @Test
    fun `test path mapping without transformers is not affected`(): Unit = runBlocking {
        val initialResponse = wiremockExtension.client.post("request-transform/json") {
            contentType(ContentType.Application.Json)
            setBody("""{"attribute": "value"}""")
        }

        assert(initialResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.stubs.single().request.bodyPatterns.single())
            .isEqualTo(
                EqualToJsonPattern(
                    """{"attribute":"modified value"}"""
                )
            )
        assert(wiremockExtension.stubs.single().response.body == """{"attribute":"modified value"}""")

        val stubResponse = wiremockExtension.client.post("request-transform/json") {
            contentType(ContentType.Application.Json)
            setBody("""{"attribute":"modified value"}""")
        }

        assert(stubResponse.status == HttpStatusCode.OK)
        assert(stubResponse.bodyAsText() == """{"attribute":"modified value"}""")
    }
}
