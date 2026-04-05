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

import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.transform.model.DateTimeTruncation
import me.velikiy.frozenflow.transform.model.DateTimeTruncation.*
import me.velikiy.frozenflow.utils.*
import java.time.*
import java.time.format.*
import java.time.temporal.*
import com.github.tomakehurst.wiremock.common.DateTimeTruncation as WmDateTimeTruncation

class RelativeDateTimePatternBuilder(
    private val requestBodyPattern: Request.Pattern,
    private val metadata: Metadata,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val format = requireNotNull(requestBodyPattern.format) {
        "Date format must be specified in relative date pattern for attribute: '${requestBodyPattern.ref}'!"
    }
    private val formatter = createFormatter(format, requestBodyPattern.ref)

    @Suppress("ForbiddenComment")
    fun build(currentValue: Any, ref: String): EqualToDateTimePattern {
        require(currentValue is String) {
            "Expected string value representation for date attribute: '$ref'!"
        }
        checkDateTimeMatchesFormat(currentValue, formatter, ref, format)

        val date = DateTimeFormatter.ofPattern(format).parseBest(
            currentValue,
            ZonedDateTime::from,
            LocalDateTime::from,
            LocalDate::from
        )

        return when (date) {
            // TODO: Implement processing of other date types
            is LocalDate -> {
                val isSystemZone = requestBodyPattern.timezone == null
                val zoneId = if (isSystemZone) clock.zone else requestBodyPattern.timezone

                val loggedDateTime = ZonedDateTime.ofInstant(metadata.loggedAt, zoneId).truncatedTo(ChronoUnit.DAYS)
                val currentDateTime = ZonedDateTime.now(clock.withZone(zoneId)).truncatedTo(ChronoUnit.DAYS)

                val loggedLocalDate = loggedDateTime.toLocalDate()
                val currentLocalDate = currentDateTime.toLocalDate()

                val yearsBetween = ChronoUnit.YEARS.between(date, loggedLocalDate)
                val daysBetween = ChronoUnit.DAYS.between(date, loggedLocalDate.minusYears(yearsBetween))
                val dateFromToday = currentLocalDate.minusYears(yearsBetween).minusDays(daysBetween)

                val offset = if (isSystemZone) {
                    val daysFromToday = ChronoUnit.DAYS.between(currentLocalDate, dateFromToday).toInt()
                    DateTimeOffset(daysFromToday, DateTimeUnit.DAYS)
                } else {
                    val dateTimeFromToday = dateFromToday.atStartOfDay(clock.zone)
                    val hoursFromToday = ChronoUnit.HOURS.between(currentDateTime, dateTimeFromToday).toInt()
                    DateTimeOffset(hoursFromToday, DateTimeUnit.HOURS)
                }

                val spec = if (offset.amount >= 0) "now +$offset" else "now $offset"

                val truncation = requestBodyPattern.truncation ?: FIRST_HOUR_OF_DAY

                EqualToDateTimePattern(spec)
                    .actualFormat<EqualToDateTimePattern>(format)
                    .truncateExpected<EqualToDateTimePattern>(truncation.toWmDateTimeTruncation())
                    .applyTruncationLast(true)
            }

            else -> error("Unsupported date type: ${date::class.simpleName} for format: $format!")
        }
    }
}

private fun DateTimeTruncation.toWmDateTimeTruncation(): WmDateTimeTruncation =
    when (this) {
        FIRST_HOUR_OF_DAY -> WmDateTimeTruncation.FIRST_HOUR_OF_DAY
        FIRST_DAY_OF_MONTH -> WmDateTimeTruncation.FIRST_DAY_OF_MONTH
        FIRST_DAY_OF_NEXT_MONTH -> WmDateTimeTruncation.FIRST_DAY_OF_NEXT_MONTH
        LAST_DAY_OF_MONTH -> WmDateTimeTruncation.LAST_DAY_OF_MONTH
        FIRST_DAY_OF_YEAR -> WmDateTimeTruncation.FIRST_DAY_OF_YEAR
        FIRST_DAY_OF_NEXT_YEAR -> WmDateTimeTruncation.FIRST_DAY_OF_NEXT_YEAR
        LAST_DAY_OF_YEAR -> WmDateTimeTruncation.LAST_DAY_OF_YEAR
    }
