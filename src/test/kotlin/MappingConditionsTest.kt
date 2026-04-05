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

import com.jayway.jsonpath.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import me.velikiy.frozenflow.content.xml.*
import me.velikiy.frozenflow.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MappingConditionsTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    private val resourcePath get() = wiremockExtension.resourcePath

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "test-headers-all-conditions-true, Another value",
        "test-headers-exists-condition-false, Some value",
        "test-headers-value-condition-false, Some value",
        "test-headers-regex-condition-false, Some value",
    )
    fun `test headers conditions`(service: String, expectedValue: String): Unit = runBlocking {
        val response = wiremockExtension.client.get("$service/headers") {
            headers {
                append("Test-Header", "Some value")
                append("Test-Empty-Header", "")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val actual = wiremockExtension.stubs.single().let { stub ->
            stub.request.headers["Test-Header"]?.expected
        }
        assertEquals(expectedValue, actual)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "test-json-all-conditions-true, Another value",
        "test-json-exists-condition-false, Some value",
        "test-json-value-condition-false, Some value",
        "test-json-regex-condition-false, Some value",
    )
    fun `test json body conditions`(service: String, expectedValue: String): Unit = runBlocking {
        val response = wiremockExtension.client.post("$service/json-body") {
            contentType(ContentType.Application.Json)
            setBody(loadStringFromClassPath("$resourcePath/json-request-body.json"))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val actual: String = wiremockExtension.stubs.single().let { stub ->
            val bodyPattern = stub.request.bodyPatterns[0].value as String
            JsonPath.parse(bodyPattern).read("$.value")
        }
        assertEquals(expectedValue, actual)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "test-xml-all-conditions-true, Another value",
        "test-xml-tag-exists-condition-false, Some value",
        "test-xml-attr-exists-condition-false, Some value",
        "test-xml-tag-value-condition-false, Some value",
        "test-xml-attr-value-condition-false, Some value",
        "test-xml-tag-regex-condition-false, Some value",
        "test-xml-attr-regex-condition-false, Some value",
    )
    fun `test xml body conditions`(service: String, expectedValue: String): Unit = runBlocking {
        val response = wiremockExtension.client.post("$service/xml-body") {
            contentType(ContentType.Text.Xml)
            setBody(loadStringFromClassPath("$resourcePath/xml-request-body.xml"))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val actual: String? = wiremockExtension.stubs.single().let { stub ->
            val bodyPattern = stub.request.bodyPatterns[0].value as String
            Xml.parse(bodyPattern).read<String>("/request/value")
        }
        assertEquals(expectedValue, actual)
    }
}
