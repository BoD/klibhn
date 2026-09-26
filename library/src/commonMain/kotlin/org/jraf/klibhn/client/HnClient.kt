/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2026-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 * and contributors (https://github.com/BoD/klibhn/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jraf.klibhn.client

import org.jraf.klibhn.internal.client.HnClientImpl
import org.jraf.klibhn.model.Story

interface HnClient : AutoCloseable {
  class Configuration(
    val http: Http = Http(),
  ) {
    class Http(
      val loggingLevel: HttpLoggingLevel = HttpLoggingLevel.NONE,
      val httpProxy: HttpProxy? = null,
    ) {
      class HttpProxy(
        val host: String,
        val port: Int,
      )

      enum class HttpLoggingLevel {
        /**
         * No logs.
         */
        NONE,
        INFO,
        HEADERS,
        BODY,
        ALL,
      }
    }
  }

  suspend fun getBestStoryIds(): Result<List<Story.Id>>

  suspend fun getStoryOverView(id: Story.Id): Result<Story.Overview>

  suspend fun getStoryOverviews(ids: List<Story.Id>): Result<List<Story.Overview>>

  suspend fun getStoryWithComments(id: Story.Id): Result<Story.WithComments>
}

fun HnClient(
  configuration: HnClient.Configuration = HnClient.Configuration(),
): HnClient = HnClientImpl(configuration)

