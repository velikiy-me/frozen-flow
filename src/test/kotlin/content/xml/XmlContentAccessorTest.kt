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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import java.util.stream.*

const val TEST_XML = """
    <bookstore>
        <book>
          <title lang="en">Harry Potter</title>
          <price>29.99</price>
        </book>
        <book>
          <title lang="en">Learning XML</title>
          <price>39.95</price>
        </book>
    </bookstore>
"""

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XmlContentAccessorTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("refArgsParamsFactory")
    fun `test args in map method`(ref: String, expectedArgs: Array<Array<String>>) {
        val context = Xml.parse(TEST_XML)
        val result: MutableList<Array<String>> = mutableListOf()

        context.map(ref) { current, _, args ->
            result.add(args)
            current
        }

        assertArrayEquals(expectedArgs, result.toTypedArray())
    }

    private fun refArgsParamsFactory(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                "bookstore",
                arrayOf(
                    emptyArray<String>()
                )
            ),
            Arguments.of(
                "/bookstore",
                arrayOf(
                    emptyArray<String>()
                )
            ),
            Arguments.of(
                "/bookstore/book",
                arrayOf(
                    arrayOf("1"),
                    arrayOf("2")
                )
            ),
            Arguments.of(
                "bookstore/book",
                arrayOf(
                    arrayOf("1"),
                    arrayOf("2")
                )
            ),
            Arguments.of(
                "//bookstore",
                arrayOf(
                    arrayOf("", "1"),
                )
            ),
            Arguments.of(
                "//book",
                arrayOf(
                    arrayOf("bookstore[1]", "1"),
                    arrayOf("bookstore[1]", "2"),
                )
            ),
            Arguments.of(
                "bookstore//book",
                arrayOf(
                    arrayOf("", "1"),
                    arrayOf("", "2"),
                )
            ),
            Arguments.of(
                "//bookstore/book",
                arrayOf(
                    arrayOf("", "1", "1"),
                    arrayOf("", "1", "2"),
                )
            ),
            Arguments.of(
                "//@lang",
                arrayOf(
                    arrayOf("bookstore[1]/book[1]/title[1]"),
                    arrayOf("bookstore[1]/book[2]/title[1]"),
                )
            ),
            Arguments.of(
                "/bookstore/book[1]",
                arrayOf(
                    emptyArray<String>()
                )
            ),
            Arguments.of(
                "/bookstore/book[last()]",
                arrayOf(
                    arrayOf("2"),
                )
            ),
            Arguments.of(
                "/bookstore/book[last()-1]",
                arrayOf(
                    arrayOf("1"),
                )
            ),
            Arguments.of(
                "/bookstore/book[position()<3]",
                arrayOf(
                    arrayOf("1"),
                    arrayOf("2"),
                )
            ),
            Arguments.of(
                "//title[@lang]",
                arrayOf(
                    arrayOf("bookstore[1]/book[1]", "1"),
                    arrayOf("bookstore[1]/book[2]", "1"),
                )
            ),
            Arguments.of(
                "//title[@lang='en']",
                arrayOf(
                    arrayOf("bookstore[1]/book[1]", "1"),
                    arrayOf("bookstore[1]/book[2]", "1"),
                )
            ),
            Arguments.of(
                "/bookstore/book[price>35.00]",
                arrayOf(
                    arrayOf("2"),
                )
            ),
            Arguments.of(
                "/bookstore/book[price>35.00]/title",
                arrayOf(
                    arrayOf("2", "1"),
                )
            ),
            Arguments.of(
                "/bookstore/*",
                arrayOf(
                    arrayOf("book[1]"),
                    arrayOf("book[2]")
                )
            ),
            Arguments.of(
                "//title[@*]",
                arrayOf(
                    arrayOf("bookstore[1]/book[1]", "1"),
                    arrayOf("bookstore[1]/book[2]", "1")
                )
            ),
//            Arguments.of(
//                "//*",
//                arrayOf(
//                    arrayOf("", "bookstore[1]"),
//                    arrayOf("bookstore", "book[1]"),
//                    arrayOf("bookstore/book[1]", "title"),
//                    arrayOf("bookstore/book[1]", "price"),
//                    arrayOf("bookstore", "book[2]"),
//                    arrayOf("bookstore/book[2]", "title"),
//                    arrayOf("bookstore/book[2]", "price")
//                )
//            ),
        )
}
