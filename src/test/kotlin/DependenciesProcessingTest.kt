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
import com.jayway.jsonpath.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import me.velikiy.frozenflow.content.xml.*
import me.velikiy.frozenflow.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DependenciesProcessingTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    @Test
    fun `dependent params values in json body are replaced in second request mappings`() =
        runBlocking {
            val usedFirstRequestResult = sendFirstRequestJson()
            sendFirstRequestJson()
            val initialResponse = sendSecondRequestJson(
                usedFirstRequestResult.result,
                usedFirstRequestResult.resultList[1]
            )

            assertEquals(HttpStatusCode.OK, initialResponse.status)
            assertEquals(
                "success",
                Json.node(initialResponse.bodyAsText()).get("result").textValue()
            )

            wiremockExtension.snapshotRecord()
            wiremockExtension.reloadInPlaybackMode()

            val firstMappingResponse = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-json/first-request-json"
            }.let {
                JsonPath.parse(it.response.body)
            }
            val secondMappingRequest = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-json/second-request-json"
            }.let {
                JsonPath.parse(it.request.bodyPatterns.first().value as String)
            }

            assertEquals(
                firstMappingResponse.read<String>("$.result"),
                secondMappingRequest.read<String>("$.id")
            )
            assertEquals(
                firstMappingResponse.read<String>("$.resultList[1]"),
                secondMappingRequest.read<String>("$.itemId")
            )

            val mockResult = sendFirstRequestJson()
            val mockResponse = sendSecondRequestJson(mockResult.result, mockResult.resultList[1])

            assertEquals(HttpStatusCode.OK, mockResponse.status)
            assertEquals("success", Json.node(mockResponse.bodyAsText()).get("result").textValue())
        }

    @Test
    fun `stubs are deduplicated in case of different order of json body attributes in the first request`() =
        runBlocking {
            val usedFirstRequestResult = sendFirstRequestJson()
            sendReorderedFirstRequestJson()
            val initialResponse = sendSecondRequestJson(
                usedFirstRequestResult.result,
                usedFirstRequestResult.resultList[1]
            )

            assertEquals(HttpStatusCode.OK, initialResponse.status)
            assertEquals(
                "success",
                Json.node(initialResponse.bodyAsText()).get("result").textValue()
            )

            wiremockExtension.snapshotRecord()
            wiremockExtension.reloadInPlaybackMode()

            val firstMappingResponse = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-json/first-request-json"
            }.let {
                JsonPath.parse(it.response.body)
            }
            val secondMappingRequest = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-json/second-request-json"
            }.let {
                JsonPath.parse(it.request.bodyPatterns.first().value as String)
            }

            assertEquals(
                firstMappingResponse.read<String>("$.result"),
                secondMappingRequest.read<String>("$.id")
            )
            assertEquals(
                firstMappingResponse.read<String>("$.resultList[1]"),
                secondMappingRequest.read<String>("$.itemId")
            )

            val mockResult = sendFirstRequestJson()
            val mockResponse = sendSecondRequestJson(mockResult.result, mockResult.resultList[1])

            assertEquals(HttpStatusCode.OK, mockResponse.status)
            assertEquals("success", Json.node(mockResponse.bodyAsText()).get("result").textValue())
        }

    private suspend fun sendFirstRequestJson(): FirstRequestResult =
        wiremockExtension.client.post("dependencies-test-json/first-request-json") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "attribute1": "value1",
                  "attribute2": "value2",
                  "attribute3": ["value2", "value1", "value3"],
                  "uniqueAttribute": "${UUID.randomUUID()}"
                }
                """.trimIndent()
            )
        }.body()

    private suspend fun sendReorderedFirstRequestJson(): FirstRequestResult =
        wiremockExtension.client.post("dependencies-test-json/first-request-json") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "attribute2": "value2",
                  "uniqueAttribute": "${UUID.randomUUID()}",
                  "attribute3": ["value2", "value1", "value3"],
                  "attribute1": "value1"
                }
                """.trimIndent()
            )
        }.body()

    private suspend fun sendSecondRequestJson(id: String, itemId: String): HttpResponse =
        wiremockExtension.client.post("dependencies-test-json/second-request-json") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "id": "$id",
                  "itemId": "$itemId"
                }
                """.trimIndent()
            )
        }

    @Test
    fun `dependent params values in xml body are replaced in second request mappings`() =
        runBlocking {
            val usedFirstRequestResult = sendFirstRequestXml()
            sendFirstRequestXml()
            val initialResponse = sendSecondRequestXml(
                usedFirstRequestResult.result,
                usedFirstRequestResult.resultList[1]
            )

            assertEquals(HttpStatusCode.OK, initialResponse.status)
            assertEquals(
                "success",
                Xml.parse(initialResponse.bodyAsText()).read("/response/result")
            )

            wiremockExtension.snapshotRecord()
            wiremockExtension.reloadInPlaybackMode()

            val firstMappingResponse = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-xml/first-request-xml"
            }.let {
                Xml.parse(it.response.body)
            }
            val secondMappingRequest = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-xml/second-request-xml"
            }.let {
                Xml.parse(it.request.bodyPatterns.first().value as String)
            }

            assertEquals(
                firstMappingResponse.read<String>("/response/@result"),
                secondMappingRequest.read<String>("request/id")
            )

            val mockResult = sendFirstRequestXml()
            val mockResponse = sendSecondRequestXml(mockResult.result, mockResult.resultList[1])

            assertEquals(HttpStatusCode.OK, mockResponse.status)
            assertEquals(
                "success",
                Xml.parse(mockResponse.bodyAsText()).read("/response/result")
            )
        }

    @Test
    fun `stubs are deduplicated in case of different order of xml body nodes and attributes in the first request`() =
        runBlocking {
            val usedFirstRequestResult = sendFirstRequestXml()
            sendReorderedFirstRequestXml()
            val initialResponse = sendSecondRequestXml(
                usedFirstRequestResult.result,
                usedFirstRequestResult.resultList[1]
            )

            assertEquals(HttpStatusCode.OK, initialResponse.status)
            assertEquals(
                "success",
                Xml.parse(initialResponse.bodyAsText()).read("/response/result")
            )

            wiremockExtension.snapshotRecord()
            wiremockExtension.reloadInPlaybackMode()

            val firstMappingResponse = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-xml/first-request-xml"
            }.let {
                Xml.parse(it.response.body)
            }
            val secondMappingRequest = wiremockExtension.stubs.single { mapping ->
                mapping.request.urlPath == "/dependencies-test-xml/second-request-xml"
            }.let {
                Xml.parse(it.request.bodyPatterns.first().value as String)
            }

            assertEquals(
                firstMappingResponse.read<String>("/response/@result"),
                secondMappingRequest.read<String>("request/id")
            )

            val mockResult = sendFirstRequestXml()
            val mockResponse = sendSecondRequestXml(mockResult.result, mockResult.resultList[1])

            assertEquals(HttpStatusCode.OK, mockResponse.status)
            assertEquals(
                "success",
                Xml.parse(mockResponse.bodyAsText()).read("/response/result")
            )
        }

    private suspend fun sendFirstRequestXml(): FirstRequestResult =
        wiremockExtension.client.post("dependencies-test-xml/first-request-xml") {
            accept(ContentType.Text.Xml)
            contentType(ContentType.Text.Xml)
            setBody(
                """
                <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE request PUBLIC "-//SomeRoot//DTD something v1//EN" "http://someinvalidDtd">
                <request>
                    <node1 attribute1="value1" attribute2="value2">value1</node1>
                    <node2>value2</node2>
                    <node3>
                        <item>value2</item>
                        <item>value1</item>
                        <item>value3</item>
                    </node3>
                    <uniqueNode>${UUID.randomUUID()}</uniqueNode>
                </request>
                """.trimIndent()
            )
        }.body()

    private suspend fun sendReorderedFirstRequestXml(): FirstRequestResult =
        wiremockExtension.client.post("dependencies-test-xml/first-request-xml") {
            accept(ContentType.Text.Xml)
            contentType(ContentType.Text.Xml)
            setBody(
                """
                <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE request PUBLIC "-//SomeRoot//DTD something v1//EN" "http://someinvalidDtd">
                <request>
                    <node2>value2</node2>
                    <uniqueNode>${UUID.randomUUID()}</uniqueNode>
                    <node3>
                        <item>value2</item>
                        <item>value1</item>
                        <item>value3</item>
                    </node3>
                    <node1 attribute2="value2" attribute1="value1">value1</node1>
                </request>
                """.trimIndent()
            )
        }.body()

    private suspend fun sendSecondRequestXml(id: String, itemId: String): HttpResponse =
        wiremockExtension.client.post("dependencies-test-xml/second-request-xml") {
            accept(ContentType.Text.Xml)
            contentType(ContentType.Text.Xml)
            setBody(
                """
                <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE request PUBLIC "-//SomeRoot//DTD something v1//EN" "http://someinvalidDtd">
                <request>
                    <id>$id</id>
                    <itemId>$itemId</itemId>
                </request>
                """.trimIndent()
            )
        }

    @Serializable
    private data class FirstRequestResult(
        val result: String,
        val resultList: List<String>,
    )
}
