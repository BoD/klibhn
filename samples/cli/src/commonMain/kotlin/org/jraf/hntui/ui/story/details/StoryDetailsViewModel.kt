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

package org.jraf.hntui.ui.story.details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jraf.hntui.data.HnRepository
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiComment
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiStory
import org.jraf.hntui.util.TuiViewModel
import org.jraf.hntui.util.htmlWrapped
import org.jraf.hntui.util.wrapped
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class StoryDetailsViewModel(
  private val hnRepository: HnRepository,
  private val storyId: Story.Id,
) : TuiViewModel() {
  sealed interface InternalState {
    object Loading : InternalState
    data class Error(val throwable: Throwable) : InternalState
    data class Content(
      val screenDimensions: Pair<Int, Int>,
      val story: Story.WithComments,
      val uiStory: UiStory,
      val uiComments: List<UiComment>,
      val focusedCommentId: Comment.Id?,
      val scroll: Int,
    ) : InternalState
  }

  sealed interface UiState {
    object Loading : UiState
    data class Error(val throwable: Throwable) : UiState
    data class Content(
      val story: UiStory,
      val comments: List<UiComment>,
      val focusedCommentId: Comment.Id?,
      val scroll: Int,
    ) : UiState {
      data class UiStory(
        val id: Story.Id,
        val creationDate: Instant,
        val titleLines: List<String>,
        val author: String,
        val score: Int,
        val textLines: List<String>,
        val urlLines: List<String>,
      )

      data class UiComment(
        val id: Comment.Id,
        val creationDate: Instant,
        val author: String,
        val indent: Int,
        val textLines: List<String>,
      )
    }
  }

  private val internalState = MutableStateFlow<InternalState>(InternalState.Loading)
  val uiState: StateFlow<UiState> = internalState.map { internalState ->
    when (internalState) {
      is InternalState.Loading -> UiState.Loading
      is InternalState.Error -> UiState.Error(internalState.throwable)
      is InternalState.Content -> {
        UiState.Content(
          story = internalState.uiStory,
          comments = internalState.uiComments,
          focusedCommentId = internalState.focusedCommentId,
          scroll = internalState.scroll,
        )
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = UiState.Loading,
  )

  fun setScreenDimensions(width: Int, height: Int) {
    when (internalState.value) {
      is InternalState.Error -> {
        return
      }

      is InternalState.Content -> {
        val state = internalState.value as InternalState.Content
        internalState.value = state.copy(
          screenDimensions = width to height,
          uiStory = state.story.toUiStory(width),
          uiComments = state.story.toUiComments(width),
          scroll = adjustScroll(state, state.focusedCommentId),
        )

      }

      is InternalState.Loading -> {
        viewModelScope.launch {
          val story = hnRepository.getStoryWithComments(storyId).getOrElse { e ->
            internalState.value = InternalState.Error(e)
            return@launch
          }
          internalState.value = InternalState.Content(
            screenDimensions = width to height,
            story = story,
            uiStory = story.toUiStory(width),
            uiComments = story.toUiComments(width),
            focusedCommentId = story.comments.firstOrNull()?.id,
            scroll = 0,
          )
        }
      }
    }
  }

  private fun Story.WithComments.toUiStory(maxWidth: Int): UiStory = UiStory(
    id = id,
    creationDate = creationDate,
    titleLines = title.htmlWrapped(maxWidth = maxWidth),
    author = author,
    score = score,
    textLines = text?.htmlWrapped(maxWidth = maxWidth) ?: emptyList(),
    urlLines = url?.wrapped(maxWidth = maxWidth) ?: emptyList(),
  )

  private fun Story.WithComments.toUiComments(maxWidth: Int): List<UiComment> {
    return buildList {
      fun addComment(comment: Comment, indent: Int) {
        add(
          UiComment(
            id = comment.id,
            creationDate = comment.creationDate,
            author = comment.author,
            indent = indent,
            textLines = comment.text.htmlWrapped(maxWidth = maxWidth - indent * 2),
          ),
        )
        for (comment in comment.comments) {
          addComment(comment, indent + 1)
        }
      }
      for (comment in comments) {
        addComment(comment, indent = 0)
      }
    }
  }

  fun focusUp() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val focusedCommentIndex = state.uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex <= 0) return
    val newFocusedCommentId = state.uiComments[focusedCommentIndex - 1].id
    val newScroll = adjustScroll(state, newFocusedCommentId)
    internalState.value = (internalState.value as InternalState.Content).copy(
      focusedCommentId = newFocusedCommentId,
      scroll = newScroll,
    )
  }

  fun focusDown() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val focusedCommentIndex = state.uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex >= state.uiComments.lastIndex) return
    val newFocusedCommentId = state.uiComments[focusedCommentIndex + 1].id
    val newScroll = adjustScroll(state, newFocusedCommentId)
    internalState.value = (internalState.value as InternalState.Content).copy(
      focusedCommentId = newFocusedCommentId,
      scroll = newScroll,
    )
  }

  private fun adjustScroll(
    state: InternalState.Content,
    newFocusedCommentId: Comment.Id?,
  ): Int {
    if (newFocusedCommentId == null) return 0
    var totalHeightWithoutLastComment = state.uiStory.titleLines.size + state.uiStory.urlLines.size + state.uiStory.textLines.size + 1
    var totalHeightWithLastComment = totalHeightWithoutLastComment
    for (comment in state.uiComments) {
      totalHeightWithLastComment += comment.textLines.size + 1
      if (comment.id == newFocusedCommentId) break
      totalHeightWithoutLastComment += comment.textLines.size + 1
    }
    val scroll = state.scroll
    return if (totalHeightWithoutLastComment < scroll) {
      if (newFocusedCommentId == state.uiComments.first().id) {
        // If the new focused comment is the first one, scroll to the top
        0
      } else {
        totalHeightWithoutLastComment
      }
    } else {
      val screenHeight = state.screenDimensions.second
      if (totalHeightWithLastComment - scroll > screenHeight) {
        totalHeightWithLastComment - screenHeight
      } else {
        scroll
      }
    }
  }
}
