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

import me.velikiy.frozenflow.init.*
import org.slf4j.LoggerFactory

const val DEFAULT_PORT = 8090
const val CONFIG_PATH = "./config"

val APPLICATION_RUN_MODE: RunMode =
    with(System.getenv()["RUN_MODE"]) {
        requireNotNull(this) { "RUN_MODE is required!" }
        RunMode.valueOf(this.uppercase())
    }
val PORT: Int = with(System.getenv()["WIREMOCK_PORT"]) { this?.toInt() ?: DEFAULT_PORT }

private val logger = LoggerFactory.getLogger("me.velikiy.frozenflow.Application")

@Suppress("TooGenericExceptionCaught")
fun main() {
    try {
        ServerInitializer(PORT, APPLICATION_RUN_MODE, CONFIG_PATH).start()
    } catch (e: Throwable) {
        logger.error("Unexpected initialization error: ${e.message}", e)
        throw e
    }
}
