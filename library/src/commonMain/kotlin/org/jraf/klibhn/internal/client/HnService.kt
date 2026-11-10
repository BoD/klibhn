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

package org.jraf.klibhn.internal.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.jraf.klibhn.internal.client.html.HnHtmlParser
import org.jraf.klibhn.internal.client.json.JsonStoryOverview
import org.jraf.klibhn.model.Story

internal class HnService(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private companion object {
    const val URL_BASE_FIREBASE = "https://hacker-news.firebaseio.com/v0"
    const val URL_BASE_YCOMBINATOR = "https://news.ycombinator.com"
  }

  private val parser by lazy { HnHtmlParser() }

  suspend fun getBestStoryIds(): List<Long> {
    return httpClient.get("$URL_BASE_FIREBASE/beststories.json").body()
  }

  suspend fun getStoryOverview(id: Long): JsonStoryOverview {
    return httpClient.get("$URL_BASE_FIREBASE/item/$id.json").body()
  }

  suspend fun getStoryWithComments(id: Long): Story.WithComments {
    val htmlStr: String = httpClient.get("$URL_BASE_YCOMBINATOR/item?id=$id").body()
    return parser.parseStoryWithComments(htmlStr)
  }

  override fun close() {
    httpClient.close()
  }
}
