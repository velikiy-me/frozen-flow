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
package me.velikiy.frozenflow.content

import java.nio.charset.*

interface Content {

    fun toString(charset: Charset = Charsets.UTF_8, format: Boolean = false, normalize: Boolean = false): String

    fun toByteArray(format: Boolean = false, normalize: Boolean = false): ByteArray
}

interface ContentFactory<T : Content> {

    fun parse(content: String, charset: Charset = Charsets.UTF_8): T

    fun parse(content: ByteArray): T
}

fun <T : Content> ContentFactory<T>.tryToParse(content: String, charset: Charset = Charsets.UTF_8): T? {
    return try {
        parse(content, charset)
    } catch (_: Exception) {
        null
    }
}

fun <T : Content> ContentFactory<T>.tryToParse(content: ByteArray): T? {
    return try {
        parse(content)
    } catch (_: Exception) {
        null
    }
}
