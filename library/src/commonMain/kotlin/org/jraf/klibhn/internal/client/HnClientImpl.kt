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

@file:OptIn(ExperimentalTime::class)

package org.jraf.klibhn.internal.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jraf.klibhn.client.HnClient
import org.jraf.klibhn.client.HnClient.Configuration.Http.HttpLoggingLevel
import org.jraf.klibhn.internal.json.JsonComment
import org.jraf.klibhn.internal.json.JsonStory
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import org.jraf.klibnanolog.logd
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class HnClientImpl(
  private var configuration: HnClient.Configuration,
) : HnClient {
  private val service: HnService by lazy {
    HnService(provideHttpClient())
  }

  private fun provideHttpClient(): HttpClient {
    return HttpClient {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            useAlternativeNames = false
          },
        )
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
      }
      engine {
        // Set up a proxy if requested
        configuration.http.httpProxy?.let { httpProxy ->
          proxy = ProxyBuilder.http(
            URLBuilder().apply {
              host = httpProxy.host
              port = httpProxy.port
            }.build(),
          )
        }
      }

      // Setup logging if requested
      if (configuration.http.loggingLevel != HttpLoggingLevel.NONE) {
        install(Logging) {
          logger = object : Logger {
            override fun log(message: String) {
              logd(message)
            }
          }
          level = when (configuration.http.loggingLevel) {
            HttpLoggingLevel.NONE -> LogLevel.NONE
            HttpLoggingLevel.INFO -> LogLevel.INFO
            HttpLoggingLevel.HEADERS -> LogLevel.HEADERS
            HttpLoggingLevel.BODY -> LogLevel.BODY
            HttpLoggingLevel.ALL -> LogLevel.ALL
          }
        }
      }
    }
  }

  override suspend fun getBestStoryIds(): List<Long> {
    return service.getBestStoryIds()
  }

  override suspend fun getStory(id: Long): Story {
    val jsonStory = service.getStory(id)
    return jsonStory.toStory()
  }

  override fun close() {
    service.close()
  }
}

private fun JsonStory.toStory() = Story(
  id = id,
  creationDate = Instant.fromEpochMilliseconds(created_at_i * 1000),
  title = title,
  author = author,
  score = points,
  text = text,
  url = url,
  comments = children.map { jsonComment ->
    jsonComment.toComment()
  },
)

private fun JsonComment.toComment(): Comment = Comment(
  id = id,
  creationDate = Instant.fromEpochMilliseconds(created_at_i * 1000),
  author = author,
  text = text,
  comments = children.map { childJsonComment ->
    childJsonComment.toComment()
  },
)
