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
package me.velikiy.frozenflow.content.xml

import me.velikiy.frozenflow.content.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*

class XmlContextTest {

    @Test
    fun `format xml string`() {
        val input =
            """<?xml version="1.0" encoding="UTF-8"?><root><child attr="value"><nested>value</nested></child></root>"""

        val result = Xml.parse(input).toString(format = true)

        assertThat(result).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root>
                <child attr="value">
                    <nested>value</nested>
                </child>
            </root>
            """.trimIndent()
        )
    }

    @Test
    fun `format already formatted xml string`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr="value">
                <nested>value</nested>
            </child>
        </root>
        """.trimIndent()

        val result = Xml.parse(input).toString(format = true)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `serialize xml string without formatting`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr="value">
                <nested>value</nested>
            </child>
        </root>
        """.trimIndent()

        val result = Xml.parse(input).toString()

        assertThat(result).isEqualTo(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>""" +
                """<root><child attr="value"><nested>value</nested></child></root>"""
        )
    }

    @Test
    fun `normalize xml string`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr2="value2" attr1="value1">
                <nested2>value2</nested2>
                <nested1>
                  <item>value2</item>
                  <item>value1</item>
                </nested1>
            </child>
        </root>
        """.trimIndent()

        val result = Xml.parse(input).toString(normalize = true)

        assertThat(result).isEqualTo(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>""" +
                """<root><child attr1="value1" attr2="value2"><nested1><item>value2</item><item>value1</item>""" +
                """</nested1><nested2>value2</nested2></child></root>"""
        )
    }

    @Test
    fun `format xml byte array`() {
        val input =
            """<?xml version="1.0" encoding="UTF-8"?><root><child attr="value"><nested>value</nested></child></root>"""
                .toByteArray(Charsets.UTF_8)

        val result = Xml.parse(input).toByteArray(format = true)

        assertThat(result).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <root>
                <child attr="value">
                    <nested>value</nested>
                </child>
            </root>
            """.trimIndent().toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `format already formatted xml byte array`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr="value">
                <nested>value</nested>
            </child>
        </root>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = Xml.parse(input).toByteArray(format = true)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `serialize byte array without formatting`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr="value">
                <nested>value</nested>
            </child>
        </root>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = Xml.parse(input).toByteArray()

        assertThat(result).isEqualTo(
            (
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?>""" +
                    """<root><child attr="value"><nested>value</nested></child></root>"""
                ).toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `normalize xml byte array`() {
        val input = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <root>
            <child attr2="value2" attr1="value1">
                <nested2>value2</nested2>
                <nested1>
                  <item>value2</item>
                  <item>value1</item>
                </nested1>
            </child>
        </root>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = Xml.parse(input).toByteArray(normalize = true)

        assertThat(result).isEqualTo(
            (
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?>""" +
                    """<root><child attr1="value1" attr2="value2"><nested1><item>value2</item><item>value1</item>""" +
                    """</nested1><nested2>value2</nested2></child></root>"""
                ).toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `tryToParse returns null for invalid xml`() {
        val input = "not xml"

        val result = Xml.tryToParse(input)

        assertThat(result).isNull()
    }
}
