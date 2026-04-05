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
import com.github.tomakehurst.wiremock.common.DateTimeTruncation
import com.github.tomakehurst.wiremock.matching.*
import me.velikiy.frozenflow.transform.model.*
import me.velikiy.frozenflow.utils.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import java.time.*

class RelativeDateTimePatternBuilderTest {

    @CsvSource(
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2025-03-03, now +0 days,",
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2025-02-26, now -5 days,",
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2024-03-03, now -365 days,",
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2024-02-27, now -370 days,",
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2025-03-03, now +8 hours, Asia/Hong_Kong",
        "2025-03-03T19:00:00Z, 2025-03-03T19:00:00Z, 2024-02-27, now -8896 hours, Asia/Hong_Kong",
        "2025-03-03T19:00:00Z, 2025-03-03T19:00:00Z, 2024-12-06, now -2104 hours, Asia/Hong_Kong",
        "2025-03-03T15:00:00Z, 2025-03-03T15:00:00Z, 2025-03-03, now -3 hours, America/Argentina/Buenos_Aires",
        "2025-01-30T18:22:56Z, 2025-01-30T18:22:56Z, 2025-01-31, now +21 hours, America/Argentina/Buenos_Aires",
        "2025-03-04T03:13:39Z, 2025-01-30T18:22:56Z, 2025-01-31, now +21 hours, America/Argentina/Buenos_Aires"
    )
    @ParameterizedTest
    fun `check pattern`(now: Instant, loggedAt: Instant, date: LocalDate, resultSpec: String, zoneId: ZoneId?) {
        val builder = RelativeDateTimePatternBuilder(
            Request.Pattern(
                ref = "$.date",
                type = "relative-date",
                format = "yyyy-MM-dd",
                timezone = zoneId,
            ),
            Metadata().apply {
                this.loggedAt = loggedAt
            },
            Clock.fixed(now, ZoneId.of("UTC")),
        )

        val pattern = builder.build(date.toString(), "$.date")

        assertThat(pattern).isEqualTo(
            EqualToDateTimePattern(resultSpec)
                .actualFormat<AbstractDateTimePattern>("yyyy-MM-dd")
                .truncateExpected<AbstractDateTimePattern>(DateTimeTruncation.FIRST_HOUR_OF_DAY)
                .applyTruncationLast(true)
        )
    }
}
