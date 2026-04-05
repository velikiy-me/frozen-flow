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
import io.ktor.http.*
import kotlinx.coroutines.*
import me.velikiy.frozenflow.test.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XmlProcessingTest {

    @RegisterExtension
    private val wiremockExtension = TestWiremockExtension()

    @Test
    fun `test embedded DTDs do not affect XML matching`(): Unit = runBlocking {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE nodeWithDefaultAttributeValue [
                <!ELEMENT nodeWithDefaultAttributeValue EMPTY>
                <!ATTLIST nodeWithDefaultAttributeValue
                    attribute  CDATA #REQUIRED
                    attributeWithDefaultValue CDATA 'defaultValue'>
            ]>
            <nodeWithDefaultAttributeValue attribute="value" />
        """.trimIndent()

        wiremockExtension.client.post("test-dtd-processing/post-xml") {
            contentType(ContentType.Text.Xml)
            setBody(content)
        }

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.loadHttpCalls().single().request.body!!.asString()).isEqualTo(
            """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <nodeWithDefaultAttributeValue attribute="value" attributeWithDefaultValue="defaultValue"/>
            """.trimIndent()
        )

        val response = wiremockExtension.client.post("test-dtd-processing/post-xml") {
            contentType(ContentType.Text.Xml)
            setBody(content)
        }

        assert(response.status == HttpStatusCode.OK)
    }

    @Test
    fun `test external DTDs do not affect XML processing and matching`(): Unit = runBlocking {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE nodeWithDefaultAttributeValue PUBLIC "r" "http://localhost:${wiremockExtension.wiremockPort}/dtd-with-default-attribute-value.dtd">
            <nodeWithDefaultAttributeValue attribute="value"/>
        """.trimIndent()

        wiremockExtension.client.post("test-dtd-processing/post-xml") {
            contentType(ContentType.Text.Xml)
            setBody(content)
        }

        wiremockExtension.snapshotRecord()
        wiremockExtension.reloadInPlaybackMode()

        assertThat(wiremockExtension.loadHttpCalls().single().request.body!!.asString()).isEqualTo(
            """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <nodeWithDefaultAttributeValue attribute="value"/>
            """.trimIndent()
        )

        val response = wiremockExtension.client.post("test-dtd-processing/post-xml") {
            contentType(ContentType.Text.Xml)
            setBody(content)
        }

        assert(response.status == HttpStatusCode.OK)
    }
}
