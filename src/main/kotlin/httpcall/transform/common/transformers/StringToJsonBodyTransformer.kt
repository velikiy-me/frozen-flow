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
package me.velikiy.frozenflow.httpcall.transform.common.transformers

import io.github.oshai.kotlinlogging.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.httpcall.transform.request.*
import me.velikiy.frozenflow.httpcall.transform.response.*

private val logger = KotlinLogging.logger { }

object StringToJsonBodyTransformer : RequestTransformer, ResponseTransformer {

    override fun transform(request: Request): Request =
        request.body?.transformToJson()?.let { request.copy(body = it) } ?: request

    override fun transformForMapping(response: Response): Response =
        response.body?.transformToJson()?.let { response.copy(body = it) } ?: response

    private fun Body.transformToJson() = stringToJson(this) ?: null.also {
        logger.warn { "No transformation applied due to an unexpected body format:\n ${asString()}" }
    }

    override fun transform(response: Response): Response =
        response.body?.let { body ->
            response.copy(body = jsonToString(body))
        } ?: response
}

private fun stringToJson(body: Body): Body? =
    body.asString()
        .takeIf { it.startsWith("\"") && it.endsWith("\"") }
        ?.let { it.substring(1, it.length - 1).replace("\\\"", "\"").replace("\\\\", "\\") }
        ?.let { body.copy(it) }

private fun jsonToString(body: Body): Body =
    body.asString()
        .let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
        .let { body.copy(it) }
