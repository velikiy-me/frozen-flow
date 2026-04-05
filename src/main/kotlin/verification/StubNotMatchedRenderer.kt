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

import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import com.github.tomakehurst.wiremock.verification.*
import com.github.tomakehurst.wiremock.verification.notmatched.*
import io.github.oshai.kotlinlogging.*
import me.velikiy.frozenflow.content.*
import me.velikiy.frozenflow.content.json.Json
import me.velikiy.frozenflow.content.xml.*
import me.velikiy.frozenflow.http.encoder.*
import me.velikiy.frozenflow.utils.*

private val logger = KotlinLogging.logger {}

private const val NOT_FOUND_HTTP_STATUS_CODE = 404

class StubNotMatchedRenderer(
    private val requestEncoder: HttpRequestEncoder,
) : NotMatchedRenderer() {

    override fun render(admin: Admin, serveEvent: ServeEvent): ResponseDefinition {
        val request = serveEvent.request
        val nearMisses = admin.findTopNearMissesFor(request).nearMisses

        val message = buildString {
            appendLine("Request was not matched:")
            appendLine()
            if (nearMisses.isNotEmpty()) {
                renderClosestMappings(nearMisses)
                appendLine()
                renderDiff(nearMisses[0])
            } else {
                appendLine("No stub mappings found for the request:")
                appendLine()
                renderRequest(request)
            }
        }

        logger.warn { message }

        return ResponseDefinitionBuilder.responseDefinition()
            .withStatus(NOT_FOUND_HTTP_STATUS_CODE)
            .withHeader(ContentTypes.CONTENT_TYPE, "text/plain")
            .withBody(message)
            .build()
    }

    @Suppress("MagicNumber")
    private fun StringBuilder.renderClosestMappings(nearMisses: List<NearMiss>) {
        appendLine("Closest stub mappings:")
        nearMisses.take(3).forEach { miss ->
            appendLine("  ${miss.stubMapping.name}")
            val httpCalls = miss.stubMapping.httpCallsOrEmpty
            if (httpCalls.isNotEmpty()) {
                appendLine("    HTTP calls:")
                httpCalls.forEach { call ->
                    appendLine("      $call")
                }
            }
        }
    }

    private fun StringBuilder.renderRequest(wmRequest: LoggedRequest) {
        append(requestEncoder.encode(wmRequest.toRequest()))
    }

    private fun StringBuilder.renderDiff(nearMiss: NearMiss) {
        val request = nearMiss.request
        val requestPattern = nearMiss.stubMapping.request

        val matchReport = MatchReport()

        with(matchReport) {
            append(processMatch(requestPattern.method, request.method))
            append(" ")
            append(processMatch(requestPattern.urlMatcher, request.url))
            appendLine()
            requestPattern.headers.forEach { (name, pattern) ->
                request.headers.getHeader(name)?.let { value ->
                    appendLine(processMatch(pattern, value, prefix = "$name: "))
                }
            }
            appendLine()
            requestPattern.bodyPatterns?.let { patterns ->
                if (patterns.isNotEmpty()) {
                    processBodyPatterns(patterns, request.bodyAsString)
                }
            }
        }

        if (matchReport.isNotEmpty()) {
            appendLine("Expected:")
            appendLine("---------")
            append(matchReport.expected)
            appendLine()
            appendLine("Actual:")
            appendLine("-------")
            append(matchReport.actual)
        }
    }

    private fun MatchReport.processBodyPatterns(
        bodyPatterns: List<ContentPattern<*>>,
        body: String,
    ) {
        val actualBody = formatBodyIfPossible(bodyPatterns, body)
        if (bodyPatterns.none { it !is PathPattern }) {
            append(MatchReport.Item(actualBody, actualBody))
        }
        var bodyRendered = false
        bodyPatterns.forEach { pattern ->
            val item = when (pattern) {
                is EqualToJsonPattern -> processMatch(
                    matcher = pattern,
                    actualValue = actualBody,
                    expectedValue = Json.tryToParse(pattern.expected)?.toString(format = true)
                        ?: pattern.expected,
                    renderActualValue = !bodyRendered
                ).also {
                    bodyRendered = true
                }

                is EqualToXmlPattern -> processMatch(
                    matcher = pattern,
                    actualValue = actualBody,
                    expectedValue = Xml.tryToParse(pattern.expected)?.toString(format = true)
                        ?: pattern.expected,
                    renderActualValue = !bodyRendered
                ).also {
                    bodyRendered = true
                }

                is PathPattern -> processPathPatternMatch(
                    matcher = pattern,
                    actualValue = actualBody
                )

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    processMatch(
                        matcher = pattern as ContentPattern<Any>,
                        actualValue = actualBody,
                        renderActualValue = !bodyRendered
                    ).also {
                        bodyRendered = true
                    }
                }
            }
            appendLine(item)
        }
    }

    private fun formatBodyIfPossible(
        bodyPatterns: List<ContentPattern<*>>,
        body: String,
    ): String =
        bodyPatterns.firstNotNullOfOrNull { pattern ->
            when (pattern) {
                is EqualToJsonPattern, is MatchesJsonPathPattern -> Json
                is EqualToXmlPattern, is MatchesXPathPattern -> Xml
                else -> null
            }
        }?.tryToParse(body)?.toString(format = true) ?: body

    private fun <T> processMatch(
        matcher: NamedValueMatcher<T>,
        actualValue: T,
        expectedValue: String? = matcher.expected,
        prefix: String = "",
        renderActualValue: Boolean = true,
    ): MatchReport.Item {

        val expected = if (matcher.match(actualValue).isExactMatch) {
            actualValue.toString()
        } else {
            prefix + expectedValue
        }
        val actual = when {
            !renderActualValue -> null
            actualValue is MultiValue -> actualValue.takeIf { it.isPresent }?.toString()
            else -> actualValue?.toString()
        }
        return MatchReport.Item(expected, actual)
    }

    private fun processPathPatternMatch(
        matcher: PathPattern,
        actualValue: String,
    ): MatchReport.Item {

        val result = matcher.getExpressionResult(actualValue)
        val actual = if (result.isNotEmpty()) {
            result.toString()
        } else {
            null
        }
        val expected = if (matcher.match(actualValue)?.isExactMatch == true) {
            actual ?: "[no expression result]"
        } else {
            "${matcher.expected} [${matcher.valuePattern.name}] ${matcher.valuePattern.expected}"
        }

        return MatchReport.Item(expected, actual)
    }
}

private class MatchReport {
    val expected = StringBuilder()
    val actual = StringBuilder()

    fun appendLine() {
        expected.appendLine()
        actual.appendLine()
    }

    fun append(value: String) {
        expected.append(value)
        actual.append(value)
    }

    fun append(item: Item) {
        expected.append(item.expected)
        item.actual?.let { actual.append(it) }
    }

    fun appendLine(item: Item) {
        expected.appendLine(item.expected)
        item.actual?.let { actual.appendLine(it) }
    }

    fun isNotEmpty(): Boolean =
        expected.isNotEmpty() || actual.isNotEmpty()

    data class Item(
        val expected: String,
        val actual: String?,
    )
}
