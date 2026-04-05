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
package me.velikiy.frozenflow.transform.support.wiremock

import com.fasterxml.jackson.annotation.*
import me.velikiy.frozenflow.content.xml.*
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern as WmEqualToXmlPattern

class EqualToXmlPattern(
    @JsonProperty("equalToXml") expectedValue: String,
) : WmEqualToXmlPattern(expectedValue, true, true) {

    private val normalizedXml = Xml.parse(expectedValue).toByteArray(normalize = true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EqualToXmlPattern

        return normalizedXml.contentEquals(other.normalizedXml)
    }

    override fun hashCode(): Int {
        return normalizedXml.contentHashCode()
    }
}
