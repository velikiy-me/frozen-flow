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
package me.velikiy.frozenflow.http.encoder

import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.headers.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultHttpCallEncoderTest {

    private val encoder = DefaultHttpCallEncoder(DefaultHttpRequestEncoder, DefaultHttpResponseEncoder)

    @Test
    fun `test single part request http call encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "Custom-Header" to "value1",
                    "Custom-Header" to "value2"
                ),
                body = """{"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}""".toByteArray()
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json"
                ),
                body = """{"result":"ok"}""".toByteArray()
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            Content-Type: application/json
            Accept: application/json
            Custom-Header: value1
            Custom-Header: value2
            
            {"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}
            
            <--
            200
            Content-Type: application/json
            
            {"result":"ok"}
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call without body encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "GET",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "Custom-Header" to "value1",
                    "Custom-Header" to "value2"
                )
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json"
                )
            )
        )
        val expectedEncoded = """
            -->
            GET /test?param1=value1,param2=value2
            Content-Type: application/json
            Accept: application/json
            Custom-Header: value1
            Custom-Header: value2
            
            <--
            200
            Content-Type: application/json
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call with empty headers encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(),
                body = """{"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}""".toByteArray()
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(),
                body = """{"result":"ok"}""".toByteArray()
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            
            eyJhdHRyaWJ1dGUxIjoidmFsdWUxIiwiYXR0cmlidXRlMiI6MCwiYXR0cmlidXRlMyI6WyJhIiwiYiJdfQ==
            
            <--
            200
            
            eyJyZXN1bHQiOiJvayJ9
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call without headers and body encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(),
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(),
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            
            <--
            200
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test single part request http call with metadata encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "Custom-Header" to "value1",
                    "Custom-Header" to "value2"
                ),
                body = """{"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}""".toByteArray(),
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json"
                ),
                body = """{"result":"ok"}""".toByteArray()
            ),
            metadata = mutableMapOf(
                "key1" to "value1",
                "key2" to "value2"
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            Content-Type: application/json
            Accept: application/json
            Custom-Header: value1
            Custom-Header: value2
            
            {"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}
            
            <--
            200
            Content-Type: application/json
            
            {"result":"ok"}
            
            ---
            key1: value1
            key2: value2
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call without headers and body and with metadata encoding `() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(),
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(),
            ),
            metadata = mutableMapOf(
                "key1" to "value1",
                "key2" to "value2"
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            
            <--
            200
            
            ---
            key1: value1
            key2: value2
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call with html response including comment encoding`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(
                    "Content-Type" to "application/json",
                    "Accept" to "*/*",
                ),
                body = """{"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}"""
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(
                    "Content-Type" to "text/html"
                ),
                body = """
                    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                    <html xmlns="http://www.w3.org/1999/xhtml">
                    <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
                    <title>Title</title>
                    <style type="text/css">
                    <!-- Commented style
                    body{margin:0;}
                    -->
                    </style>
                    </head>
                    <body>
                    Body
                    </body>
                    </html>
                """.trimIndent()
            )
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            Content-Type: application/json
            Accept: */*
            
            {"attribute1":"value1","attribute2":0,"attribute3":["a","b"]}
            
            <--
            200
            Content-Type: text/html
            
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
            <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
            <title>Title</title>
            <style type="text/css">
            <!-- Commented style
            body{margin:0;}
            -->
            </style>
            </head>
            <body>
            Body
            </body>
            </html>
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    @Test
    fun `test http call with xml request response including comment encoding with metadata`() {
        val httpCall = HttpCall(
            request = Request(
                method = "POST",
                uri = "/test?param1=value1,param2=value2",
                headers = HttpHeaders.of(
                    "Content-Type" to "text/xml",
                    "Accept" to "text/xml",
                ),
                body = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <request>
                    <attribute>value</attribute>
                    <!--
                    <commentedAttribute>value</commentedAttribute>
                    -->
                    </request>
                """.trimIndent()
            ),
            response = Response(
                status = 200,
                headers = HttpHeaders.of(
                    "Content-Type" to "text/xml"
                ),
                body = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <response>
                    <attribute>value</attribute>
                    <!--
                    <commentedAttribute>value</commentedAttribute>
                    -->
                    </response>
                """.trimIndent()
            ),
            metadata = mutableMapOf("key1" to "value1", "key2" to "value2")
        )
        val expectedEncoded = """
            -->
            POST /test?param1=value1,param2=value2
            Content-Type: text/xml
            Accept: text/xml
            
            <?xml version="1.0" encoding="UTF-8"?>
            <request>
            <attribute>value</attribute>
            <!--
            <commentedAttribute>value</commentedAttribute>
            -->
            </request>
            
            <--
            200
            Content-Type: text/xml
            
            <?xml version="1.0" encoding="UTF-8"?>
            <response>
            <attribute>value</attribute>
            <!--
            <commentedAttribute>value</commentedAttribute>
            -->
            </response>
            
            ---
            key1: value1
            key2: value2
            
        """.trimIndent()

        check(httpCall, expectedEncoded)
    }

    private fun check(httpCall: HttpCall, expectedEncoded: String) {

        val encoded = encoder.encode(httpCall)

        assertThat(encoded).isEqualTo(expectedEncoded)

        val decoded = encoder.decode(encoded)

        assertThat(decoded).isEqualTo(httpCall)
    }
}
