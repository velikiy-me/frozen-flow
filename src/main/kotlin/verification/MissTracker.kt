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
package me.velikiy.frozenflow.verification

import java.util.concurrent.*
import java.util.concurrent.atomic.*

data class MissReport(
    val service: String,
    val uri: String,
    val method: String,
    val message: String,
    val timestamp: Long,
)

class MissTracker(
    private val maxReports: Int?,
) {
    private val _totalMisses = AtomicLong(0)
    private val _reports = ConcurrentLinkedDeque<MissReport>()

    val total: Long get() = _totalMisses.get()
    val misses: List<MissReport> get() = _reports.toList()

    fun register(report: MissReport) {
        _totalMisses.incrementAndGet()
        _reports.addFirst(report)
        while (maxReports != null && _reports.size > maxReports) {
            _reports.removeLast()
        }
    }

    fun reset() {
        _totalMisses.set(0)
        _reports.clear()
    }
}
