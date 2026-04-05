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
package me.velikiy.frozenflow.test

import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import me.velikiy.frozenflow.init.*
import org.junit.jupiter.api.extension.*
import java.nio.file.*
import kotlin.io.path.*

class TestWiremockExtension(
    private val runMode: RunMode = RunMode.RECORDING,
    private val path: String? = null,
) : BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

    private lateinit var _resourcePath: String
    val resourcePath get() = _resourcePath
    val wiremockPort = computeRandomPort()
    val targetServerPort = computeRandomPort()
    private lateinit var wiremockServer: ServerInitializer
    private var targetServer: WireMockServer? = null
    val client = HttpClient(Java) {
        install(ContentNegotiation) {
            json()
            xml(contentType = ContentType.Text.Xml)
        }
        defaultRequest {
            url("http://localhost:$wiremockPort/")
        }
    }
    val stubs: List<StubMapping> get() = wiremockServer.stubs

    fun reloadInPlaybackMode() {
        reload(RunMode.PLAYBACK)
    }

    fun reload(runMode: RunMode) {
        wiremockServer.stop()
        wiremockServer = initProxyWiremock(wiremockPort, targetServerPort, runMode)
        wiremockServer.start(false)
    }

    private fun initProxyWiremock(
        port: Int,
        targetPort: Int,
        runMode: RunMode,
    ): ServerInitializer {
        val config = initConfig(resourcePath, targetPort)
        return ServerInitializer(port, runMode, "src/test/resources$resourcePath", config)
    }

    private fun initConfig(configPath: String, targetPort: Int): Config {
        val config = Config.load({}.javaClass.getResource(configPath)!!.path)
        return config.copy(
            services = config.services.mapValues { (_, service) ->
                service.copy(
                    proxyBaseUrl = service.proxyBaseUrl.replace("{port}", targetPort.toString())
                )
            }
        )
    }

    private fun initTargetWiremock(
        proxyPort: Int,
    ) = WireMockServer(
        WireMockConfiguration.wireMockConfig()
            .port(proxyPort)
            .globalTemplating(true)
            .usingFilesUnderDirectory("src/test/resources$resourcePath/target/")
    )

    private fun start() {
        if (runMode != RunMode.PLAYBACK) {
            targetServer = initTargetWiremock(targetServerPort)
            targetServer!!.start()
        }
        wiremockServer = initProxyWiremock(wiremockPort, targetServerPort, runMode)
        wiremockServer.start(false)
    }

    private fun stop() {
        wiremockServer.stop()
        clearStoredStubs()
        targetServer?.stop()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun clearStoredStubs() {
        Paths.get(wiremockServer.fileSource.path).deleteRecursively()
    }

    fun snapshotRecord() {
        wiremockServer.snapshot()
    }

    fun containsStubWithBodyPattern(patterns: List<ContentPattern<*>>): Boolean =
        wiremockServer.stubs.any {
            it.request.bodyPatterns.containsAll(patterns)
        }

    fun loadHttpCalls() = wiremockServer.loadHttpCalls()

    override fun beforeAll(context: ExtensionContext) {
        _resourcePath = buildPathToTestResources(context.requiredTestClass) + path?.let { "/$path" }.orEmpty()
    }

    override fun beforeEach(context: ExtensionContext) {
        start()
    }

    override fun afterEach(context: ExtensionContext) {
        stop()
    }

    override fun afterAll(context: ExtensionContext) {
        client.close()
    }
}
