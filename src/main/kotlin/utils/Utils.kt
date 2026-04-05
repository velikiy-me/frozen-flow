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
package me.velikiy.frozenflow.utils

import java.nio.file.*
import kotlin.io.path.*

@Suppress("NestedBlockDepth")
fun copyDirectoryContents(sourceDir: Path, targetDir: Path) {
    if (sourceDir.notExists() || !sourceDir.isDirectory()) return

    Files.walk(sourceDir).use { paths ->
        paths.forEach { sourcePath ->
            val destinationPath = targetDir.resolve(sourceDir.relativize(sourcePath))
            if (sourcePath.isDirectory()) {
                if (destinationPath.notExists()) {
                    destinationPath.createDirectories()
                }
            } else {
                sourcePath.copyTo(destinationPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

fun deleteDirectoryContents(path: Path) {
    if (path.notExists() || !path.isDirectory()) return

    Files.newDirectoryStream(path).use { paths ->
        paths.forEach { path ->
            if (Files.isDirectory(path)) {
                deleteDirectoryContents(path)
            }
            Files.delete(path)
        }
    }
}
