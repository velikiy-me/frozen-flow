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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import java.util.stream.*

const val TEST_JSON = """
{
    "store": {
        "book": [
            {
                "category": "reference",
                "author": "Nigel Rees",
                "title": "Sayings of the Century",
                "price": 8.95
            },
            {
                "category": "fiction",
                "author": "Evelyn Waugh",
                "title": "Sword of Honour",
                "price": 12.99
            },
            {
                "category": "fiction",
                "author": "Herman Melville",
                "title": "Moby Dick",
                "isbn": "0-553-21311-3",
                "price": 8.99
            },
            {
                "category": "fiction",
                "author": "J. R. R. Tolkien",
                "title": "The Lord of the Rings",
                "isbn": "0-395-19395-8",
                "price": 22.99
            }
        ],
        "bicycle": {
            "color": "red",
            "price": 19.95
        }
    },
    "expensive": 10
}
"""

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonContentAccessorTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("refArgsParamsFactory")
    fun `test args in map method`(ref: String, expectedArgs: Array<Array<String>>) {
        val context = Json.parse(TEST_JSON)
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
                "\$.store.book[*].author",
                arrayOf(
                    arrayOf("0"),
                    arrayOf("1"),
                    arrayOf("2"),
                    arrayOf("3")
                )
            ),
            Arguments.of(
                "\$..author",
                arrayOf(
                    arrayOf("['store']['book'][0]"),
                    arrayOf("['store']['book'][1]"),
                    arrayOf("['store']['book'][2]"),
                    arrayOf("['store']['book'][3]")
                )
            ),
            Arguments.of(
                "\$.store.*",
                arrayOf(
                    arrayOf("book"),
                    arrayOf("bicycle")
                )
            ),
            Arguments.of(
                "\$.store..price",
                arrayOf(
                    arrayOf("['book'][0]"),
                    arrayOf("['book'][1]"),
                    arrayOf("['book'][2]"),
                    arrayOf("['book'][3]"),
                    arrayOf("['bicycle']")
                )
            ),
            Arguments.of(
                "\$..book[2]",
                arrayOf(
                    arrayOf("['store']")
                )
            ),
            Arguments.of(
                "\$..book[0,1]",
                arrayOf(
                    arrayOf("['store']", "0"),
                    arrayOf("['store']", "1")
                )
            ),
            Arguments.of(
                "\$..book[:2]",
                arrayOf(
                    arrayOf("['store']", "0"),
                    arrayOf("['store']", "1")
                )
            ),
            Arguments.of(
                "\$..book[1:2]",
                arrayOf(
                    arrayOf("['store']", "1")
                )
            ),
            Arguments.of(
                "\$..book[-2:]",
                arrayOf(
                    arrayOf("['store']", "2"),
                    arrayOf("['store']", "3")
                )
            ),
            Arguments.of(
                "\$..book[2:]",
                arrayOf(
                    arrayOf("['store']", "2"),
                    arrayOf("['store']", "3")
                )
            ),
            Arguments.of(
                "\$..book[?(@.isbn)]",
                arrayOf(
                    arrayOf("['store']", "2"),
                    arrayOf("['store']", "3")
                )
            ),
            Arguments.of(
                "\$.store.book[?(@.price < 10)]",
                arrayOf(
                    arrayOf("0"),
                    arrayOf("2")
                )
            ),
            Arguments.of(
                "\$..book[?(@.price <= \$['expensive'])]",
                arrayOf(
                    arrayOf("['store']", "0"),
                    arrayOf("['store']", "2")
                )
            ),
            Arguments.of(
                "\$..book[?(@.author =~ /.*REES/i)]",
                arrayOf(
                    arrayOf("['store']", "0")
                )
            ),
            Arguments.of(
                "\$..*",
                arrayOf(
                    arrayOf("", "store"),
                    arrayOf("", "expensive"),
                    arrayOf("['store']", "book"),
                    arrayOf("['store']", "bicycle"),
                    arrayOf("['store']['book']", "0"),
                    arrayOf("['store']['book']", "1"),
                    arrayOf("['store']['book']", "2"),
                    arrayOf("['store']['book']", "3"),
                    arrayOf("['store']['book'][0]", "category"),
                    arrayOf("['store']['book'][0]", "author"),
                    arrayOf("['store']['book'][0]", "title"),
                    arrayOf("['store']['book'][0]", "price"),
                    arrayOf("['store']['book'][1]", "category"),
                    arrayOf("['store']['book'][1]", "author"),
                    arrayOf("['store']['book'][1]", "title"),
                    arrayOf("['store']['book'][1]", "price"),
                    arrayOf("['store']['book'][2]", "category"),
                    arrayOf("['store']['book'][2]", "author"),
                    arrayOf("['store']['book'][2]", "title"),
                    arrayOf("['store']['book'][2]", "isbn"),
                    arrayOf("['store']['book'][2]", "price"),
                    arrayOf("['store']['book'][3]", "category"),
                    arrayOf("['store']['book'][3]", "author"),
                    arrayOf("['store']['book'][3]", "title"),
                    arrayOf("['store']['book'][3]", "isbn"),
                    arrayOf("['store']['book'][3]", "price"),
                    arrayOf("['store']['bicycle']", "color"),
                    arrayOf("['store']['bicycle']", "price")
                )
            ),
            Arguments.of(
                "\$.store.book[2].isbn",
                arrayOf(
                    emptyArray<String>()
                )
            )
        )
}
