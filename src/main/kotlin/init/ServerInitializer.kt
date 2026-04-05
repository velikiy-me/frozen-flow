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
package me.velikiy.frozenflow.init

import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.common.*
import com.github.tomakehurst.wiremock.common.filemaker.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.core.WireMockApp.*
import com.github.tomakehurst.wiremock.recording.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.http.encoder.*
import me.velikiy.frozenflow.httpcall.transform.request.wiremock.*
import me.velikiy.frozenflow.httpcall.transform.response.wiremock.*
import me.velikiy.frozenflow.snapshot.*
import me.velikiy.frozenflow.staticstub.*
import me.velikiy.frozenflow.transform.*
import me.velikiy.frozenflow.utils.*
import me.velikiy.frozenflow.verification.*
import org.wiremock.extensions.state.*
import java.nio.file.*
import kotlin.io.path.*

const val MAPPING_FILENAME_TEMPLATE = "{{{method}}}-{{{url}}}-{{{id}}}.json"
const val HTTP_CALL_RECORDS_DIR = "records"
const val PROCESSED_MAPPINGS_DIR = "processed"
const val DATA_DIR = "data"
const val PLAYBACK_MAPPINGS_PATH = "mappings/playback"
const val PROXY_MAPPINGS_PATH = "mappings/proxy"
const val ALL_MAPPINGS_PATH = "mappings/all"
const val CONFIG_FILES_DIR = "files"
const val TARGET_FILES_DIR = "static"
const val SETTINGS_FILE = "settings.yaml"

@Suppress("TooManyFunctions")
class ServerInitializer(
    private val port: Int,
    private val runMode: RunMode,
    private val configPath: String,
    private val config: Config = Config.load(configPath),
) {
    private val playbackMappingsRepository = StaticMappingRepository(Path("$configPath/$PLAYBACK_MAPPINGS_PATH"))
    private val proxyMappingsRepository = StaticMappingRepository(Path("$configPath/$PROXY_MAPPINGS_PATH"))
    private val allMappingsRepository = StaticMappingRepository(Path("$configPath/$ALL_MAPPINGS_PATH"))
    val fileSource = SingleRootFileSource(DATA_DIR).apply {
        createIfNecessary()
    }
    private val fileNameMaker = object : FilenameMaker(MAPPING_FILENAME_TEMPLATE) {
        override fun filenameFor(stubMapping: StubMapping): String {
            return stubMapping.getServiceName() + "/" + stubMapping.loggedAt.toCompactFormat() + "_" +
                super.filenameFor(stubMapping)
        }
    }
    private val requestEncoder: HttpRequestEncoder = DefaultHttpRequestEncoder
    private val responseEncoder: HttpResponseEncoder = DefaultHttpResponseEncoder
    private val httpCallEncoder: HttpCallEncoder = DefaultHttpCallEncoder(requestEncoder, responseEncoder)
    private val httpCallRepository = HttpCallRepository(
        fileSource.child(MAPPINGS_ROOT).child(HTTP_CALL_RECORDS_DIR).apply {
            createIfNecessary()
        },
        httpCallEncoder
    )
    private val stubGenerator = StubGenerator(config.snapshot.headers)
    private val missTracker = MissTracker(config.general.maxMissReports)
    private val server: WireMockServer = initServer()
    private val snapshotService = SnapshotService(
        server,
        httpCallRepository
    )
    val stubs: List<StubMapping>
        get() = server.stubMappings.filter {
            it.isPersistent == true
        }

    private fun initServer(): WireMockServer {

        val wiremockConfig = WireMockConfiguration
            .wireMockConfig()
            .port(port)
            .fileSource(fileSource)
            .globalTemplating(true)
            .mappingSource(NoOpMappingSource)
            .notMatchedRendererFactory { _ -> StubNotMatchedRenderer(requestEncoder) }

        return when (runMode) {
            RunMode.RECORDING -> {
                wiremockConfig.enableSnapshots()
                WireMockServer(wiremockConfig).apply {
                    createProxyStubs()
                    allMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                    proxyMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                }
            }

            RunMode.BYPASS -> {
                wiremockConfig.disableRequestJournal()
                WireMockServer(wiremockConfig).apply {
                    createProxyStubs()
                    allMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                    proxyMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                }
            }

            RunMode.PLAYBACK -> {
                wiremockConfig.disableRequestJournal()
                wiremockConfig.configureTransformers()
                wiremockConfig.enableStateSupport()
                wiremockConfig.enableMissTracking()
                WireMockServer(wiremockConfig).apply {
                    processHttpCalls()
                    allMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                    playbackMappingsRepository.getAll().forEach { stub ->
                        addStubMapping(stub)
                    }
                }
            }
        }.apply {
            copyFiles()
        }
    }

    private fun WireMockConfiguration.enableSnapshots() {
        extensions(
            SnapshotAdminApiExtension(
                this@ServerInitializer
            )
        )
    }

    private fun WireMockConfiguration.configureTransformers() = apply {
        config.services
            .flatMap { (name, service) ->
                val serviceTransformersNotEmpty = service.transformers.request.isNotEmpty()
                service.paths
                    .filter { serviceTransformersNotEmpty || it.transformers.request.isNotEmpty() }
                    .map { path ->
                        "/$name${stripPath(path.path)}" to service.transformers.request + path.transformers.request
                    }
            }
            .toMap()
            .let { configuration ->
                if (configuration.isNotEmpty()) {
                    extensions(
                        RequestTransformingFilter(configuration)
                    )
                }
            }
        config.services
            .flatMap { (name, service) ->
                val serviceTransformersNotEmpty = service.transformers.response.isNotEmpty()
                service.paths
                    .filter { serviceTransformersNotEmpty || it.transformers.response.isNotEmpty() }
                    .map { path ->
                        "/$name${stripPath(path.path)}" to service.transformers.response + path.transformers.response
                    }
            }
            .toMap()
            .let { configuration ->
                if (configuration.isNotEmpty()) {
                    extensions(
                        ResponseDefinitionTransformer(configuration)
                    )
                }
            }
    }

    private fun WireMockConfiguration.enableStateSupport() {
        extensions(StandaloneStateExtension())
    }

    private fun WireMockConfiguration.enableMissTracking() {
        extensions(
            MissEventListener(missTracker),
            MissesAdminApiExtension(missTracker),
        )
    }

    private fun stripPath(path: String): String =
        path.replaceFirst("/$".toRegex(), "")

    private fun WireMockServer.processHttpCalls() {
        val processedFileSource = fileSource.child(MAPPINGS_ROOT).child(PROCESSED_MAPPINGS_DIR).apply {
            createIfNecessary()
            clear()
        }
        val httpCalls = httpCallRepository.loadAll()
        if (httpCalls.isEmpty()) {
            return
        }
        val services = config.services.mapValues { (name, service) ->
            service.toModel(name)
        }
        HttpCallMappingProcessor(stubGenerator, services).process(httpCalls).reversed().forEach { stub ->
            addStubMapping(stub)
            processedFileSource.writeTextFile(fileNameMaker.filenameFor(stub), Json.writePrivate(stub))
        }
    }

    private fun WireMockServer.createProxyStubs() {
        config.services.forEach { (name, service) ->
            stubFor(
                WireMock.any(WireMock.urlMatching("/$name(/.*)?"))
                    .willReturn(
                        WireMock.aResponse().proxiedFrom(service.proxyBaseUrl)
                            .withProxyUrlPrefixToRemove("/$name")
                    )
            )
        }
    }

    private fun WireMockServer.copyFiles() {
        val configFilesDir = Paths.get(configPath, CONFIG_FILES_DIR)

        val targetSource = options.filesRoot()
            .child(FILES_ROOT)
            .child(TARGET_FILES_DIR)
        targetSource.clear()

        copyDirectoryContents(configFilesDir, Path(targetSource.path))
    }

    @Suppress("TooGenericExceptionCaught")
    fun start(addShutdownHook: Boolean = true) {
        try {
            server.start()
            if (addShutdownHook) {
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        stop()
                    }
                )
            }
        } catch (e: Exception) {
            stop()
            throw e
        }
    }

    fun stop() {
        println("Server is stopping...")
        server.stop()
    }

    fun snapshot(name: String? = null): SnapshotRecordResult =
        snapshotService.takeSnapshot(name).also {
            server.processHttpCalls()
            server.resetRequests()
        }

    fun loadHttpCalls() = httpCallRepository.loadAll()
}

enum class RunMode {
    RECORDING, PLAYBACK, BYPASS
}
