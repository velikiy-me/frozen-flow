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
package me.velikiy.frozenflow.snapshot

import com.github.tomakehurst.wiremock.common.*
import me.velikiy.frozenflow.http.*
import me.velikiy.frozenflow.http.encoder.*
import me.velikiy.frozenflow.utils.*
import java.net.*
import java.util.concurrent.locks.*

private const val FILE_EXTENSION = "call"

class HttpCallRepository(
    private val fileSource: FileSource,
    private val encoder: HttpCallEncoder,
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    fun loadAll(): List<HttpCall> {
        lock.readLock().lock()
        return try {
            loadHttpCalls()
        } finally {
            lock.readLock().unlock()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadHttpCalls(): List<HttpCall> {
        if (!fileSource.exists()) {
            return emptyList()
        }
        return fileSource.listFilesRecursively().asSequence()
            .filter(AbstractFileSource.byFileExtension(FILE_EXTENSION)::test)
            .map { file ->
                try {
                    encoder.decode(file.readContentsAsString())
                } catch (e: RuntimeException) {
                    throw HttpCallLoadingException(file.path, e)
                }
            }.toList()
    }

    fun saveAll(calls: List<HttpCall>, directory: String) {
        lock.writeLock().lock()
        try {
            saveHttpCalls(calls, directory)
        } finally {
            lock.writeLock().unlock()
        }
    }

    private fun saveHttpCalls(calls: List<HttpCall>, directory: String) {
        val childSource = fileSource.child(directory)
        if (childSource.exists()) {
            childSource.clear()
        } else {
            childSource.createIfNecessary()
        }
        calls.forEach { call ->
            childSource.writeTextFile(buildFilename(call), encoder.encode(call))
        }
    }

    private fun buildFilename(call: HttpCall): String {
        val loggedAt = call.loggedAt.toCompactFormat()
        val normalizedPath = URI.create(call.request.uri).path.substring(1).replace('/', '_')
        return "${call.serviceName}/${loggedAt}_${call.request.method}_${normalizedPath}_${call.id}.$FILE_EXTENSION"
    }
}

class HttpCallLoadingException(path: String, cause: Exception) : RuntimeException(
    "Error loading http call file '$path': ${cause.message}",
    cause
)
