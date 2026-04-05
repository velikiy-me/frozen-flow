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

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.matching.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import me.velikiy.frozenflow.test.*
import me.velikiy.frozenflow.transform.request.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.*
import org.xmlunit.builder.*
import java.time.*
import java.time.temporal.*
import kotlin.Pair

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MappingModificationTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    private val resourcePath get() = wiremockExtension.resourcePath

    @Test
    fun `test headers modification`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.get("test-headers-mappings/headers") {
            headers {
                append("Custom-Value-Request-Header", "exact value")
                append("Custom-Regex-Request-Header", "some_value_123")
                append("Custom-Uuid-Request-Header", "c8074499-6999-4d97-a5a7-c38674c98878")
            }
        }

        assertEquals(HttpStatusCode.OK, proxyResponse.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.stubs.single().request.headers).isEqualTo(
            mapOf(
                "Accept" to MultiValuePattern.of(EqualToPattern("application/json,text/xml")),
                "Custom-Value-Request-Header" to MultiValuePattern.of(EqualToPattern("exact value")),
                "Custom-Regex-Request-Header" to MultiValuePattern.of(RegexPattern("""^[a-zA-Z]+_[a-zA-Z]+_\d{3}$""")),
                "Custom-Uuid-Request-Header" to MultiValuePattern.of(RegexPattern("^$UUID_REGEX$")),
            )
        )

        val stubSuccessResponse = wiremockExtension.client.get("test-headers-mappings/headers") {
            headers {
                append("Custom-Value-Request-Header", "exact value")
                append("Custom-Regex-Request-Header", "another_value_345")
                append("Custom-Uuid-Request-Header", "5284d70e-7dc3-428c-b74e-ffd176f230f6")
            }
        }

        assertEquals(HttpStatusCode.OK, stubSuccessResponse.status)
        assertEquals("updated value", stubSuccessResponse.headers["Custom-Response-Header"])
        assertEquals("04.04.2024", stubSuccessResponse.headers["Custom-Template-Response-Header"])

        val stubFailedResponse = wiremockExtension.client.get("test-headers-mappings/headers") {
            headers {
                append("Custom-Value-Request-Header", "other value")
                append("Custom-Regex-Request-Header", "another_value_345")
                append("Custom-Uuid-Request-Header", "5284d70e-7dc3-428c-b74e-ffd176f230f6")
            }
        }

        assertEquals(HttpStatusCode.NotFound, stubFailedResponse.status)
    }

    @Test
    fun `test query params modification`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.get("test-query-params-mappings/params") {
            parameter("param1", "exact_value")
            parameter("param2", "test_456")
            parameter("param3", "c8074499-6999-4d97-a5a7-c38674c98878")
            parameter("encodedParam", "encoded param&1+2")
        }

        assertEquals(HttpStatusCode.OK, proxyResponse.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertEquals(
            mapOf(
                "param1" to MultiValuePattern.of(EqualToPattern("exact_value")),
                "param2" to MultiValuePattern.of(RegexPattern("""^test_\d{3}$""")),
                "param3" to MultiValuePattern.of(RegexPattern("^$UUID_REGEX$")),
                "encodedParam" to MultiValuePattern.of(EqualToPattern("encoded param&1+2"))
            ),
            wiremockExtension.stubs.single().request.queryParameters
        )

        val stubSuccessResponse = wiremockExtension.client.get("test-query-params-mappings/params") {
            parameter("param1", "exact_value")
            parameter("param2", "test_789")
            parameter("param3", "5284d70e-7dc3-428c-b74e-ffd176f230f6")
            parameter("encodedParam", "encoded param&1+2")
        }

        assertEquals(HttpStatusCode.OK, stubSuccessResponse.status)

        val stubFailedResponse = wiremockExtension.client.get("test-query-params-mappings/params") {
            parameter("param1", "wrong_value")
            parameter("param2", "test_789")
            parameter("param3", "5284d70e-7dc3-428c-b74e-ffd176f230f6")
            parameter("encodedParam", "encoded param&1+2")
        }

        assertEquals(HttpStatusCode.NotFound, stubFailedResponse.status)
    }

    @Test
    fun `test json request modification`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-json-mappings/json-body") {
            contentType(ContentType.Application.Json)
            setBody(loadStringFromClassPath("$resourcePath/json-request-body.json"))
        }

        assertEquals(HttpStatusCode.OK, proxyResponse.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val bodyPatterns = loadContentPatternsFromClassPath(
            "$resourcePath/json-expected-request-body-patterns.json",
            *mappingPlaceholders
        )

        assertTrue(wiremockExtension.containsStubWithBodyPattern(bodyPatterns))

        val stubResponse = wiremockExtension.client.post("test-json-mappings/json-body") {
            contentType(ContentType.Application.Json)
            setBody(loadStringFromClassPath("$resourcePath/json-request-body.json"))
        }

        assertEquals(HttpStatusCode.OK, stubResponse.status)
        assertEquals(
            Json.node(loadStringFromClassPath("$resourcePath/json-expected-response-body.json")),
            Json.node(String(stubResponse.bodyAsChannel().toByteArray()))
        )
    }

    @Test
    fun `test xml request modification`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-xml-mappings/xml-body") {
            contentType(ContentType.Text.Xml)
            setBody(loadStringFromClassPath("$resourcePath/xml-request-body.xml"))
        }

        assertEquals(HttpStatusCode.OK, proxyResponse.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val bodyPatterns: List<ContentPattern<*>> = loadContentPatternsFromClassPath(
            "$resourcePath/xml-expected-request-body-patterns.json",
            *mappingPlaceholders
        )

        assertTrue(wiremockExtension.containsStubWithBodyPattern(bodyPatterns))

        val stubResponse = wiremockExtension.client.post("test-xml-mappings/xml-body") {
            contentType(ContentType.Text.Xml)
            setBody(loadStringFromClassPath("$resourcePath/xml-request-body.xml"))
        }

        assertEquals(HttpStatusCode.OK, stubResponse.status)

        val expectedBody = loadStringFromClassPath("$resourcePath/xml-expected-response-body.xml")
        val diff = DiffBuilder.compare(Input.fromString(expectedBody))
            .withTest(Input.fromByteArray(stubResponse.bodyAsChannel().toByteArray()))
            .ignoreWhitespace()
            .build()
        assertFalse(diff.hasDifferences()) {
            "Diff: ${diff.fullDescription()}"
        }
    }

    private val mappingPlaceholders = arrayOf(
        Pair(
            "##dateShift1",
            ChronoUnit.DAYS.between(
                LocalDate.now(),
                LocalDate.of(2023, 5, 1)
            ).toString()
        ),
        Pair(
            "##dateShift2",
            ChronoUnit.DAYS.between(
                LocalDate.now(),
                LocalDate.of(2023, 7, 1)
            ).toString()
        ),
        Pair(
            "##dateShift3",
            ChronoUnit.DAYS.between(
                LocalDate.now(),
                LocalDate.of(2024, 1, 1)
            ).toString()
        ),
        Pair(
            "##timeShift1",
            ChronoUnit.HOURS.between(
                ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong")).truncatedTo(ChronoUnit.DAYS),
                LocalDate.of(2024, 12, 6).atStartOfDay(ZoneId.systemDefault())
            ).toString()
        )
    )

    @Test
    fun `test json relative zoned date pattern`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-json-mappings/json-body-relative-date") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                    {
                        "zonedRelativeDate": "2024-12-06"
                    }
                """.trimIndent()
            )
        }

        assert(proxyResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        sequenceOf(
            "2024-12-05" to 404,
            "2024-12-06" to 200,
            "2024-12-07" to 404,
        ).forEach { (date, status) ->
            val stubResponse = wiremockExtension.client.post("test-json-mappings/json-body-relative-date") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "zonedRelativeDate": "$date"
                        }
                    """.trimIndent()
                )
            }
            assert(stubResponse.status.value == status)
        }
    }

    @Test
    fun `test xml relative zoned date pattern`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-xml-mappings/xml-body-relative-date") {
            contentType(ContentType.Text.Xml)
            setBody(
                """
                    <request>
                        <zonedRelativeDate>2024-12-06</zonedRelativeDate>
                        <zonedRelativeDateAttr attr="2024-12-06"/>
                    </request>
                """.trimIndent()
            )
        }

        assert(proxyResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        sequenceOf(
            "2024-12-05" to 404,
            "2024-12-06" to 200,
            "2024-12-07" to 404,
        ).forEach { (date, status) ->
            val stubResponse = wiremockExtension.client.post("test-xml-mappings/xml-body-relative-date") {
                contentType(ContentType.Text.Xml)
                setBody(
                    """
                        <request>
                            <zonedRelativeDate>$date</zonedRelativeDate>
                            <zonedRelativeDateAttr attr="$date"/>
                        </request>
                    """.trimIndent()
                )
            }
            assert(stubResponse.status.value == status)
        }
    }

    @Test
    fun `test json relative date pattern with truncation to first day of month`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-json-mappings/json-body-relative-date") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                    {
                        "truncatedRelativeDate": "2025-04-18"
                    }
                """.trimIndent()
            )
        }

        assert(proxyResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        sequenceOf(
            "2025-03-31" to 404,
            "2025-04-01" to 200,
            "2025-04-02" to 404,
        ).forEach { (date, status) ->
            val stubResponse = wiremockExtension.client.post("test-json-mappings/json-body-relative-date") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "truncatedRelativeDate": "$date"
                        }
                    """.trimIndent()
                )
            }
            assert(stubResponse.status.value == status)
        }
    }

    @Test
    fun `test xml relative date pattern with truncation to first day of month`(): Unit = runBlocking {
        val proxyResponse = wiremockExtension.client.post("test-xml-mappings/xml-body-relative-date") {
            contentType(ContentType.Text.Xml)
            setBody(
                """
                    <request>
                        <truncatedRelativeDate>2025-04-18</truncatedRelativeDate>
                        <truncatedRelativeDateAttr attr="2025-04-18"/>
                    </request>
                """.trimIndent()
            )
        }

        assert(proxyResponse.status == HttpStatusCode.OK)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        sequenceOf(
            "2025-03-31" to 404,
            "2025-04-01" to 200,
            "2025-04-02" to 404,
        ).forEach { (date, status) ->
            val stubResponse = wiremockExtension.client.post("test-xml-mappings/xml-body-relative-date") {
                contentType(ContentType.Text.Xml)
                setBody(
                    """
                        <request>
                            <truncatedRelativeDate>$date</truncatedRelativeDate>
                            <truncatedRelativeDateAttr attr="$date"/>
                        </request>
                    """.trimIndent()
                )
            }
            assert(stubResponse.status.value == status)
        }
    }
}
