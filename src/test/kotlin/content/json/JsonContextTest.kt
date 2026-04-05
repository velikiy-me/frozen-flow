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
package me.velikiy.frozenflow.content.json

import me.velikiy.frozenflow.content.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*

class JsonContextTest {

    @Test
    fun `format json string`() {
        val input = """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":"value2"}}"""

        val result = Json.parse(input).toString(format = true)

        assertThat(result).isEqualTo(
            """
            {
              "attr1" : "value1",
              "attr2" : {
                "childAttr1" : "value1",
                "childAttr2" : "value2"
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `format already formatted json string`() {
        val input = """
        {
          "attr1" : "value1",
          "attr2" : {
            "attr1" : "value1",
            "childAttr2" : "value2"
          }
        }
        """.trimIndent()

        val result = Json.parse(input).toString(format = true)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `serialize json string without formatting`() {
        val input = """
        {
          "attr1" : "value1",
          "attr2" : {
            "childAttr1" : "value1",
            "childAttr2" : "value2"
          }
        }
        """.trimIndent()

        val result = Json.parse(input).toString()

        assertThat(result).isEqualTo(
            """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":"value2"}}"""
        )
    }

    @Test
    fun `normalize json string`() {
        val input =
            """{"attr2":{"childAttr2":["value2","value1"],"childAttr1":"value1"},"attr1":"value1"}"""

        val result = Json.parse(input).toString(normalize = true)

        assertThat(result).isEqualTo(
            """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":["value2","value1"]}}"""
        )
    }

    @Test
    fun `format json byte array`() {
        val input = """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":"value2"}}"""
            .toByteArray(Charsets.UTF_8)

        val result = Json.parse(input).toByteArray(format = true)

        assertThat(result).isEqualTo(
            """
            {
              "attr1" : "value1",
              "attr2" : {
                "childAttr1" : "value1",
                "childAttr2" : "value2"
              }
            }
            """.trimIndent().toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `format already formatted json byte array`() {
        val input = """
        {
          "attr1" : "value1",
          "attr2" : {
            "childAttr1" : "value1",
            "childAttr2" : "value2"
          }
        }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = Json.parse(input).toByteArray(format = true)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `serialize byte array without formatting`() {
        val input = """
        {
          "attr1" : "value1",
          "attr2" : {
            "childAttr1" : "value1",
            "childAttr2" : "value2"
          }
        }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = Json.parse(input).toByteArray()

        assertThat(result).isEqualTo(
            """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":"value2"}}"""
                .toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `normalize json byte array`() {
        val input =
            """{"attr2":{"childAttr2":["value2","value1"],"childAttr1":"value1"},"attr1":"value1"}"""
                .toByteArray(Charsets.UTF_8)

        val result = Json.parse(input).toByteArray(normalize = true)

        assertThat(result).isEqualTo(
            """{"attr1":"value1","attr2":{"childAttr1":"value1","childAttr2":["value2","value1"]}}"""
                .toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `tryToParse returns null for invalid json`() {
        val input = "not json"

        val result = Json.tryToParse(input)

        assertThat(result).isNull()
    }
}
