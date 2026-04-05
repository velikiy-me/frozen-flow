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
package me.velikiy.frozenflow.transform.support

import com.github.tomakehurst.wiremock.matching.*
import com.github.tomakehurst.wiremock.stubbing.*
import me.velikiy.frozenflow.content.*
import me.velikiy.frozenflow.transform.support.json.*
import me.velikiy.frozenflow.transform.support.xml.*

interface RequestBodyAccessor : ContentAccessor, AutoCloseable {

    companion object {
        fun of(stub: StubMapping): RequestBodyAccessor {
            return when (val body = stub.request.bodyPatterns[0]) {
                is EqualToJsonPattern -> JsonRequestBodyAccessor(stub)
                is EqualToXmlPattern -> XmlRequestBodyAccessor(stub)
                else -> error("Unknown request body pattern type: '${body::class.simpleName}'!")
            }
        }
    }
}
