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
package me.velikiy.frozenflow.http.utils

interface MultiStringMap : Map<String, List<String>> {

    fun put(name: String, vararg values: String)

    fun add(name: String, vararg values: String)
}

open class MultiStringMapImpl(
    private val caseInsensitive: Boolean = false,
) : MultiStringMap {

    private val delegate: MutableMap<Key, MutableList<String>> = mutableMapOf()

    override fun put(name: String, vararg values: String) {
        delegate[wrap(name)] = values.toMutableList()
    }

    override fun add(name: String, vararg values: String) {
        delegate.computeIfAbsent(wrap(name)) { _ -> mutableListOf() }.addAll(values)
    }

    override val entries: Set<Map.Entry<String, List<String>>> get() =
        delegate.entries.map { (key, value) -> EntryImpl(key.key, value) }.toSet()

    override val keys: Set<String> get() = delegate.keys.map { it.key }.toSet()

    override val size: Int get() = delegate.size

    override val values: Collection<List<String>> get() = delegate.values

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun get(key: String): List<String>? = delegate[wrap(key)]

    override fun containsValue(value: List<String>): Boolean = delegate.containsValue(value)

    override fun containsKey(key: String): Boolean = delegate.containsKey(wrap(key))

    override fun toString(): String {
        return delegate.mapKeys { (key, _) -> key.key }.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiStringMapImpl

        return delegate == other.delegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    private fun wrap(key: String): Key = if (caseInsensitive) {
        Key.CaseInsensitive(key)
    } else {
        Key.CaseSensitive(key)
    }

    private data class EntryImpl(
        override val key: String,
        override val value: List<String>,
    ) : Map.Entry<String, List<String>>
}

private sealed interface Key {

    val key: String

    class CaseInsensitive(override val key: String) : Key {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CaseInsensitive

            return key.equals(other.key, ignoreCase = true)
        }

        override fun hashCode(): Int {
            return key.lowercase().hashCode()
        }
    }

    @JvmInline
    value class CaseSensitive(override val key: String) : Key
}
