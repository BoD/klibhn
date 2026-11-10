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

package org.jraf.klibhn.sample.simple

import org.jraf.klibhn.client.HnClient
import org.jraf.klibhn.client.HnClient.Configuration.Http.LoggingLevel
import org.jraf.klibnanolog.logd

class Simple {
  private val hnClient by lazy {
    HnClient(
      HnClient.Configuration(
        HnClient.Configuration.Http(
          loggingLevel = LoggingLevel.INFO,
        ),
      ),
    )
  }

  suspend fun start() {
    hnClient.use { hnClient ->
      val bestStoryIds = hnClient.getBestStoryIds().getOrThrow()
      logd(bestStoryIds)

      val storyOverview = hnClient.getStoryOverView(bestStoryIds.first()).getOrThrow()
      logd(storyOverview)

      val storyWithComments = hnClient.getStoryWithComments(bestStoryIds.first()).getOrThrow()
      logd(storyWithComments)
    }
  }
}
