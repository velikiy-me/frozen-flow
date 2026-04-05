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
package me.velikiy.frozenflow.transform.request.body.processor.common

import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.transform.model.*
import java.time.format.*

private val PLACEHOLDER_PATTERN = """\$\{.*}""".toRegex()

class DateTimeComparingPatternBuilder(
    private val requestBodyPattern: Request.Pattern,
) {
    private val format = requireNotNull(requestBodyPattern.format) {
        "Date format must be specified in date/time comparing pattern for attribute: '${requestBodyPattern.ref}'!"
    }
    private val formatter = createFormatterIfNeeded(format, requestBodyPattern.ref)
    private val value = requestBodyPattern.value as? String ?: throw IllegalArgumentException(
        "Expected string value representation for pattern value attribute: '${requestBodyPattern.ref}'!"
    )

    fun build(currentValue: Any, ref: String): StringValuePattern {
        val isPlaceholder = currentValue is String && currentValue.matches(PLACEHOLDER_PATTERN)
        if (!isPlaceholder) {
            if (format == "unix") {
                checkUnixTimestamp(currentValue, ref)
            } else {
                require(currentValue is String) {
                    "Expected string value representation for date attribute: '$ref'!"
                }
                checkDateTimeMatchesFormat(currentValue, formatter!!, ref, format)
            }
        }
        return when (requestBodyPattern.type) {
            "before" -> BeforeDateTimePattern(value).actualFormat<BeforeDateTimePattern>(format)
            "after" -> AfterDateTimePattern(value).actualFormat<AfterDateTimePattern>(format)
            else -> throw IllegalArgumentException(
                "Unsupported date/time comparing pattern type: '${requestBodyPattern.type}'!"
            )
        }
    }

    private fun createFormatterIfNeeded(format: String, ref: String): DateTimeFormatter? =
        if (format == "unix") {
            null
        } else {
            createFormatter(format, ref)
        }

    private fun checkUnixTimestamp(timestamp: Any, ref: String) {
        val longValue = when (timestamp) {
            is String -> timestamp.toLongOrNull()
                ?: throw IllegalArgumentException(
                    "Invalid timestamp value: '$timestamp' for date attribute: '$ref'!"
                )

            is Int -> timestamp.toLong()

            is Long -> timestamp

            else -> throw IllegalArgumentException(
                "Unexpected timestamp value type: '${timestamp.javaClass.name}' for date attribute: '$ref'!"
            )
        }
        require(longValue >= 0L) { "Timestamp must be positive for date attribute: '$ref'!" }
    }
}
