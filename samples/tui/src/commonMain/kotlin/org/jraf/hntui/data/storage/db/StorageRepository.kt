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

package org.jraf.hntui.data.storage.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.jraf.hntui.data.storage.db.hn.HnDatabase
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story

private const val ITEM_TYPE_STORY = 0L
private const val ITEM_TYPE_COMMENT = 1L

class StorageRepository(dbFile: Path) {
  private val database: HnDatabase = HnDatabase(createSqlDriver(dbFile))

  fun getViewedStoryIds(): Flow<Set<Story.Id>> {
    return database.viewedItemsQueries.select(ITEM_TYPE_STORY)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map { rows ->
        rows.map { Story.Id(it) }.toSet()
      }
  }

  fun getViewedCommentIds(): Flow<Set<Comment.Id>> {
    return database.viewedItemsQueries.select(ITEM_TYPE_COMMENT)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map { rows ->
        rows.map { Comment.Id(it) }.toSet()
      }
  }

  suspend fun markStoryAsViewed(storyId: Story.Id) {
    withContext(Dispatchers.IO) {
      database.viewedItemsQueries.insert(storyId.id, ITEM_TYPE_STORY).await()
    }
  }

  suspend fun markCommentAsViewed(commentId: Comment.Id) {
    withContext(Dispatchers.IO) {
      database.viewedItemsQueries.insert(commentId.id, ITEM_TYPE_COMMENT).await()
    }
  }
}
