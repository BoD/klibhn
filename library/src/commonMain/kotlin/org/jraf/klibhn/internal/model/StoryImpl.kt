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

package org.jraf.klibhn.internal.model

import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal interface StoryImpl {
  data class Overview(
    override val id: Story.Id,
    override val creationDate: Instant,
    override val title: String,
    override val author: String,
    override val score: Int,
    override val text: String?,
    override val url: String?,
    override val commentCount: Int,
  ) : Story.Overview

  data class WithComments(
    override val id: Story.Id,
    override val creationDate: Instant,
    override val title: String,
    override val author: String,
    override val score: Int,
    override val text: String?,
    override val url: String?,
    override val comments: List<Comment>,
  ) : Story.WithComments
}
