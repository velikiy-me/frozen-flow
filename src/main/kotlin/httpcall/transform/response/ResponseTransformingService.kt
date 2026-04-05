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
package me.velikiy.frozenflow.httpcall.transform.response

import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.httpcall.transform.common.transformers.*

object ResponseTransformingService {

    fun applyTransformersForMapping(response: Response, transformers: List<String>): Response =
        applyTransformers(response, transformers, ResponseTransformer::transformForMapping)

    fun applyTransformers(response: Response, transformers: List<String>): Response =
        applyTransformers(response, transformers, ResponseTransformer::transform)

    private fun applyTransformers(
        response: Response,
        transformers: List<String>,
        operation: ResponseTransformer.(Response) -> Response,
    ): Response =
        transformers
            .map { getTransformer(it) }
            .fold(response) { transformedResponse, transformer ->
                transformer.operation(transformedResponse)
            }

    private fun getTransformer(name: String): ResponseTransformer =
        when (name) {
            "string-to-json" -> StringToJsonBodyTransformer
            else -> error("Unknown request transformer: '$name'!")
        }
}
