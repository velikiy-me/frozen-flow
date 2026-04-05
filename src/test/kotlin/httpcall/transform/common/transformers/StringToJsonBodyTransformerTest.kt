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
package me.velikiy.frozenflow.httpcall.transform.common.transformers

import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.headers.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StringToJsonBodyTransformerTest {

    private val jsonContentType = ContentType("application/json")
    private val escapedJsonString = """"{\"message\":\"Hello,\\n world\\\\!\"}""""
    private val json = """{"message":"Hello,\n world\\!"}"""

    @Test
    fun `test request transformation from string to json`() {
        // Given a request with a string body that should be transformed to JSON
        val request = Request(
            method = "POST",
            uri = "/test",
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            ),
            body = Body(escapedJsonString, jsonContentType)
        )

        // When the transformer transforms the request
        val transformedRequest = StringToJsonBodyTransformer.transform(request)

        // Then the body should be transformed to JSON
        val transformedBody = transformedRequest.body?.asString()
        assertThat(transformedBody).isEqualTo(json)
    }

    @Test
    fun `test request transformation with non-transformable body`() {
        // Given a request with a body that doesn't match the transformation criteria
        val request = Request(
            method = "POST",
            uri = "/test",
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            ),
            body = Body("not a transformable string", jsonContentType)
        )

        // When the transformer transforms the request
        val transformedRequest = StringToJsonBodyTransformer.transform(request)

        // Then the body should remain unchanged
        assertThat(transformedRequest.body?.asString()).isEqualTo("not a transformable string")
    }

    @Test
    fun `test response transformation for mapping from string to json`() {
        // Given a response with a string body that should be transformed to JSON
        val response = Response(
            status = 200,
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            ),
            body = Body(escapedJsonString, jsonContentType)
        )

        // When the transformer transforms the response for mapping
        val transformedResponse = StringToJsonBodyTransformer.transformForMapping(response)

        // Then the body should be transformed to JSON
        val transformedBody = transformedResponse.body?.asString()
        assertThat(transformedBody).isEqualTo(json)
    }

    @Test
    fun `test response transformation from json to string`() {
        // Given a response with a JSON body
        val response = Response(
            status = 200,
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            ),
            body = Body(json, jsonContentType)
        )

        // When the transformer transforms the response
        val transformedResponse = StringToJsonBodyTransformer.transform(response)

        // Then the body should be transformed to a string
        assertThat(transformedResponse.body?.asString()).isEqualTo(escapedJsonString)
    }

    @Test
    fun `test request transformation with null body`() {
        // Given a request with a null body
        val request = Request(
            method = "POST",
            uri = "/test",
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            )
        )

        // When the transformer transforms the request
        val transformedRequest = StringToJsonBodyTransformer.transform(request)

        // Then the request should remain unchanged
        assertThat(transformedRequest).isEqualTo(request)
    }

    @Test
    fun `test response transformation with null body`() {
        // Given a response with a null body
        val response = Response(
            status = 200,
            headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            )
        )

        // When the transformer transforms the response
        val transformedResponse = StringToJsonBodyTransformer.transform(response)

        // Then the response should remain unchanged
        assertThat(transformedResponse).isEqualTo(response)
    }
}
