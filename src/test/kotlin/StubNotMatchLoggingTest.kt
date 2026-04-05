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
import me.velikiy.frozenflow.init.*
import me.velikiy.frozenflow.test.*
import me.velikiy.frozenflow.transform.request.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StubNotMatchLoggingTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension(path = "with_mappings")

    @RegisterExtension
    private val noMappingsWiremockExtension = TestWiremockExtension(RunMode.PLAYBACK, "no_mappings")

    @Test
    fun `test request was not matched response for json content`(): Unit = runBlocking {
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Json)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""{"attr1":"value1","attr2":"value1","attr3":"value1","attr4":"value1"}""")
        }
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Json)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""{"attr1":"value1","attr2":"value2","attr3":"value2","attr4":"value2"}""")
        }
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Json)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""{"attr1":"value2","attr2":"value2","attr3":"value2","attr4":"value2"}""")
        }

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val body = wiremockExtension.client.put("unknown-service/unknown-path") {
            contentType(ContentType.Application.Json)
            header("Custom-Header", "unknown value")
            header("Non-Existing-Header", "value")
            setBody("""{"nonExistingAttr":"value1","attr3":"nonExpectedValue","attr4":"value2"}""")
        }.body<String>()

        assertThat(body).matches(
            """
            \QRequest was not matched:
            
            Closest stub mappings:
              \E$UUID_REGEX\Q
                HTTP calls:
                  \E$UUID_REGEX\Q
              \E$UUID_REGEX\Q
                HTTP calls:
                  \E$UUID_REGEX\Q
                  \E$UUID_REGEX\Q
            
            Expected:
            ---------
            POST /test-not-match-logging/some-path
            Absent-Header: value
            Accept: application/json,text/xml
            Content-Type: application/json
            Custom-Header: value
            
            {
              "attr1" : "value2",
              "attr2" : "${'$'}{json-unit.any-string}",
              "attr3" : "${'$'}{json-unit.any-string}",
              "attr4" : "${'$'}{json-unit.any-string}"
            }
            $['attr2'] [matches] value2
            $['attr3'] [matches] value2
            value2
            
            Actual:
            -------
            PUT /unknown-service/unknown-path
            Accept: application/json,text/xml
            Content-Type: application/json
            Custom-Header: unknown value
            
            {
              "nonExistingAttr" : "value1",
              "attr3" : "nonExpectedValue",
              "attr4" : "value2"
            }
            nonExpectedValue
            value2\E
            
            """.trimIndent()
        )
    }

    @Test
    fun `test no stub found response for json content`(): Unit = runBlocking {
        val body = noMappingsWiremockExtension.client.put("test-not-match-logging") {
            contentType(ContentType.Application.Json)
            setBody("""{"attr1":"val1"}""")
        }.body<String>()

        assertThat(body).matches(
            """
            Request was not matched:
            
            No stub mappings found for the request:
            
            PUT /test-not-match-logging
            Content-Length: 16
            Host: localhost:\d{5}
            Accept: application/json,text/xml
            Accept-Charset: UTF-8
            Content-Type: application/json
            User-Agent: ktor-client
            
            \Q{
              "attr1" : "val1"
            }\E
            
            """.trimIndent()
        )
    }

    @Test
    fun `test request was not matched response for xml content`(): Unit = runBlocking {
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Xml)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""<root attr1="value1" attr2="value1" attr3="value1"><node1>value1</node1></root>""")
        }
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Xml)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""<root attr1="value1" attr2="value2" attr3="value2"><node1>value2</node1></root>""")
        }
        wiremockExtension.client.post("test-not-match-logging/some-path") {
            contentType(ContentType.Application.Xml)
            header("Custom-Header", "value")
            header("Absent-Header", "value")
            setBody("""<root attr1="value2" attr2="value2" attr3="value2"><node1>value2</node1></root>""")
        }

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        val body = wiremockExtension.client.put("unknown-service/unknown-path") {
            contentType(ContentType.Application.Xml)
            header("Custom-Header", "unknown value")
            header("Non-Existing-Header", "value")
            setBody(
                """<root attr1="value1" attr2="nonExpectedValue" attr3="value2">""" +
                    """<nonExistingNode>value1</nonExistingNode></root>"""
            )
        }.body<String>()

        assertThat(body).matches(
            """
            \QRequest was not matched:
            
            Closest stub mappings:
              \E$UUID_REGEX\Q
                HTTP calls:
                  \E$UUID_REGEX\Q
              \E$UUID_REGEX\Q
                HTTP calls:
                  \E$UUID_REGEX\Q
                  \E$UUID_REGEX\Q
            
            Expected:
            ---------
            POST /test-not-match-logging/some-path
            Absent-Header: value
            Accept: application/json,text/xml
            Content-Type: application/xml
            Custom-Header: value
            
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root attr1="value2" attr2="${'$'}{xmlunit.ignore}" attr3="${'$'}{xmlunit.ignore}">
                <node1>${'$'}{xmlunit.ignore}</node1>
            </root>
            /root[1]/node1[1]/text() [matches] value2
            /root[1]/@attr2 [matches] value2
            value2
            
            Actual:
            -------
            PUT /unknown-service/unknown-path
            Accept: application/json,text/xml
            Content-Type: application/xml
            Custom-Header: unknown value
            
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root attr1="value1" attr2="nonExpectedValue" attr3="value2">
                <nonExistingNode>value1</nonExistingNode>
            </root>
            nonExpectedValue
            value2\E
            
            """.trimIndent()
        )
    }

    @Test
    fun `test no stub found response for xml content`(): Unit = runBlocking {
        val body = noMappingsWiremockExtension.client.put("test-not-match-logging") {
            contentType(ContentType.Application.Xml)
            setBody("""<root attribute1="value1"><node1>value1</node1></root>""")
        }.body<String>()

        assertThat(body).matches(
            """
            Request was not matched:
            
            No stub mappings found for the request:
            
            PUT /test-not-match-logging
            Content-Length: 54
            Host: localhost:\d{5}
            Accept: application/json,text/xml
            Accept-Charset: UTF-8
            Content-Type: application/xml
            User-Agent: ktor-client
            
            \Q<?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root attribute1="value1">
                <node1>value1</node1>
            </root>\E
            
            """.trimIndent()
        )
    }
}
