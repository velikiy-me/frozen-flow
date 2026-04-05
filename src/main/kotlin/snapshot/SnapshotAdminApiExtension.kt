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

import com.github.tomakehurst.wiremock.admin.*
import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.http.*
import me.velikiy.frozenflow.init.*

class SnapshotAdminApiExtension(
    private val serverInitializer: ServerInitializer,
) : AdminApiExtension {

    override fun getName() = "snapshot-admin-api-extension"

    override fun contributeAdminApiRoutes(router: Router) {
        router.add(
            RequestMethod.POST,
            "/extended/recordings/snapshot",
            SnapshotTask(serverInitializer)
        )
    }
}
