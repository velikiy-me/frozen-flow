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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import java.util.*

class BrotliTest {

    @Test
    fun `test decode`() {
        // "H" (single character) compressed with Brotli
        // Base64: CwCASAM= (actually 0x0B 0x00 0x80 0x48 0x03)
        val compressed = Base64.getDecoder().decode("CwCASAM=")

        val decompressed = Brotli.decode(compressed)

        Assertions.assertThat(decompressed).isEqualTo("H".toByteArray())
    }
}
