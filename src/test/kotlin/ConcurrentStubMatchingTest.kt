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
import kotlinx.coroutines.*
import me.velikiy.frozenflow.init.*
import me.velikiy.frozenflow.test.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentStubMatchingTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension(RunMode.PLAYBACK)

    @Test
    fun `test concurrent xml body matching`(): Unit = runBlocking {
        repeat(3) {
            launch {
                val body = wiremockExtension.client.post("xml-request-mapping") {
                    headers {
                        append("Content-Type", "application/xml")
                    }
                    setBody(
                        """
                        <request><element>value</element></request>
                        """.trimIndent()
                    )
                }.body<String>()

                assertThat(body).isEqualTo("<response><result>ok</result></response>")
            }
        }
    }

    @Test
    fun `test concurrent json body matching`(): Unit = runBlocking {
        repeat(3) {
            launch {
                val body = wiremockExtension.client.post("json-request-mapping") {
                    headers {
                        append("Content-Type", "application/json")
                    }
                    setBody(
                        """
                        {"element":"value"}
                        """.trimIndent()
                    )
                }.body<String>()

                assertThat(body).isEqualTo("""{"result":"ok"}""")
            }
        }
    }
}
