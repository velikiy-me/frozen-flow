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

import com.github.tomakehurst.wiremock.admin.*
import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.common.url.*
import com.github.tomakehurst.wiremock.core.*
import com.github.tomakehurst.wiremock.extension.*
import com.github.tomakehurst.wiremock.http.*
import com.github.tomakehurst.wiremock.stubbing.*
import java.net.*

class MissesAdminApiExtension(
    private val missTracker: MissTracker,
) : AdminApiExtension {

    override fun getName() = "misses-admin-api-extension"

    override fun contributeAdminApiRoutes(router: Router) {
        router.add(
            RequestMethod.GET,
            "/extended/misses",
            GetMissesTask(missTracker)
        )
        router.add(
            RequestMethod.POST,
            "/extended/misses/reset",
            ResetMissesTask(missTracker)
        )
    }
}

class GetMissesTask(
    private val missTracker: MissTracker,
) : AdminTask {

    override fun execute(
        admin: Admin,
        serveEvent: ServeEvent,
        pathParams: PathParams,
    ): ResponseDefinition {
        return ResponseDefinitionBuilder.jsonResponse(
            MissesResponse(
                total = missTracker.total,
                misses = missTracker.misses
            ),
            HttpURLConnection.HTTP_OK
        )
    }
}

private data class MissesResponse(
    val total: Long,
    val misses: List<MissReport>,
)

class ResetMissesTask(
    private val missTracker: MissTracker,
) : AdminTask {

    override fun execute(
        admin: Admin,
        serveEvent: ServeEvent,
        pathParams: PathParams,
    ): ResponseDefinition {
        missTracker.reset()
        return ResponseDefinitionBuilder.responseDefinition()
            .withStatus(HttpURLConnection.HTTP_NO_CONTENT)
            .build()
    }
}
