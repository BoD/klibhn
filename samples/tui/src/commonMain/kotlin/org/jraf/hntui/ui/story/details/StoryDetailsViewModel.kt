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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jraf.hntui.data.hnapi.HnApiRepository
import org.jraf.hntui.data.storage.db.StorageRepository
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiComment
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiStory
import org.jraf.hntui.util.Dimensions
import org.jraf.hntui.util.TuiViewModel
import org.jraf.hntui.util.htmlWrapped
import org.jraf.hntui.util.wrapped
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val MAX_COMMENT_WIDTH = 120
private const val SCROLL_ADDITIONAL_MARGIN_FACTOR = .15

class StoryDetailsViewModel(
  private val hnApiRepository: HnApiRepository,
  private val storageRepository: StorageRepository,
  private val storyId: Story.Id,
) : TuiViewModel() {
  companion object {
    const val INDENT_WIDTH = 2
  }

  sealed interface StoryLoadingState {
    object Loading : StoryLoadingState
    data class Error(val throwable: Throwable) : StoryLoadingState
    data class Content(
      val story: Story.WithComments,
      val commentsById: Map<Comment.Id, Comment>,
    ) : StoryLoadingState
  }

  sealed interface UiState {
    object Loading : UiState
    data class Error(val throwable: Throwable) : UiState
    data class Content(
      val story: UiStory,
      val comments: List<UiComment>,
      val totalCommentCount: Int,
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
        val isCollapsed: Boolean,
        val childCount: Int,
        val isViewed: Boolean,
      )
    }
  }

  private val storyLoadingState = flow {
    hnApiRepository.getStoryWithComments(storyId)
      .onFailure {
        emit(StoryLoadingState.Error(it))
      }
      .onSuccess { story ->
        // Focus the first comment, and mark it as viewed
        story.comments.firstOrNull()?.let {
          focusedCommentId.value = it.id
          markCommentAsViewed(it.id)
        }

        // Collapse all comments by default
        collapsedCommentIds.value = story.allCollapsibleCommentIds()

        emit(StoryLoadingState.Content(story = story, commentsById = story.commentsById()))
      }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = StoryLoadingState.Loading,
  )

  private val screenDimensions = MutableStateFlow<Dimensions?>(null)
  private val focusedCommentId = MutableStateFlow<Comment.Id?>(null)
  private val scroll = MutableStateFlow(0)
  private val collapsedCommentIds = MutableStateFlow<Set<Comment.Id>>(emptySet())

  private val viewedCommentIds: Flow<Set<Comment.Id>> = storageRepository.getViewedCommentIds()

  private val uiStory: StateFlow<UiStory?> = combine(
    storyLoadingState.filterIsInstance(StoryLoadingState.Content::class),
    screenDimensions.filterNotNull(),
  ) { storyLoadingState, screenDimensions ->
    storyLoadingState.story.toUiStory(screenDimensions.width)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = null,
  )

  private val uiComments: StateFlow<List<UiComment>> = combine(
    storyLoadingState.filterIsInstance(StoryLoadingState.Content::class),
    screenDimensions.filterNotNull(),
    collapsedCommentIds,
  ) { storyLoadingState, screenDimensions, collapsedCommentIds ->
    storyLoadingState.story.toUiComments(
      maxWidth = screenDimensions.width,
      collapsedCommentIds = collapsedCommentIds,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = emptyList(),
  )

  @Suppress("UNCHECKED_CAST")
  val uiState: StateFlow<UiState> = combine(
    storyLoadingState,
    uiStory.filterNotNull(),
    uiComments,
    focusedCommentId,
    scroll,
    viewedCommentIds,
  ) { array ->
    val storyLoadingState = array[0] as StoryLoadingState
    val uiStory = array[1] as UiStory
    val uiComments = array[2] as List<UiComment>
    val focusedCommentId = array[3] as Comment.Id?
    val scroll = array[4] as Int
    val viewedCommentIds = array[5] as Set<Comment.Id>
    when (storyLoadingState) {
      is StoryLoadingState.Loading -> UiState.Loading
      is StoryLoadingState.Error -> UiState.Error(storyLoadingState.throwable)
      is StoryLoadingState.Content -> {
        UiState.Content(
          story = uiStory,
          comments = uiComments.map {
            it.copy(isViewed = it.id in viewedCommentIds)
          },
          totalCommentCount = storyLoadingState.story.commentCount,
          focusedCommentId = focusedCommentId,
          scroll = scroll,
        )
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = UiState.Loading,
  )

  fun setScreenDimensions(dimensions: Dimensions) {
    screenDimensions.value = dimensions
  }

  private fun Comment.allCollapsibleCommentIds(): Set<Comment.Id> {
    val collapsedCommentIds = mutableSetOf<Comment.Id>()
    fun addCollapsedCommentIds(comments: List<Comment>) {
      for (comment in comments) {
        if (comment.comments.isNotEmpty()) {
          collapsedCommentIds.add(comment.id)
          addCollapsedCommentIds(comment.comments)
        }
      }
    }
    if (comments.isNotEmpty()) {
      collapsedCommentIds.add(id)
      addCollapsedCommentIds(comments)
    }
    return collapsedCommentIds
  }

  private fun Story.WithComments.allCollapsibleCommentIds(): Set<Comment.Id> {
    return comments.flatMap { it.allCollapsibleCommentIds() }.toSet()
  }

  private fun Story.WithComments.commentsById(): Map<Comment.Id, Comment> {
    val commentsById = mutableMapOf<Comment.Id, Comment>()
    fun addComments(comments: List<Comment>) {
      for (comment in comments) {
        commentsById[comment.id] = comment
        addComments(comment.comments)
      }
    }
    addComments(comments)
    return commentsById
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

  private fun Story.WithComments.toUiComments(
    maxWidth: Int,
    collapsedCommentIds: Set<Comment.Id>,
  ): List<UiComment> {
    return buildList {
      fun addComment(comment: Comment, indent: Int) {
        val isCollapsed = comment.id in collapsedCommentIds
        add(
          UiComment(
            id = comment.id,
            creationDate = comment.creationDate,
            author = comment.author,
            indent = indent,
            textLines = comment.text.htmlWrapped(maxWidth = (maxWidth - indent * INDENT_WIDTH).coerceAtMost(MAX_COMMENT_WIDTH)),
            isCollapsed = isCollapsed,
            childCount = comment.comments.size,
            isViewed = false, // This will be updated later in the flow
          ),
        )
        if (!isCollapsed) {
          for (c in comment.comments) {
            addComment(c, indent + 1)
          }
        }
      }
      for (comment in comments) {
        addComment(comment, indent = 0)
      }
    }
  }

  fun onUpKeyPressed() {
    if (storyLoadingState.value !is StoryLoadingState.Content) return
    val focusedCommentId = focusedCommentId.value ?: return
    val uiComments = uiComments.value
    val focusedCommentIndex = uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex <= 0) return
    val newFocusedCommentId = uiComments[focusedCommentIndex - 1].id
    this.focusedCommentId.value = newFocusedCommentId
    scroll.value = adjustScroll(newFocusedCommentId)
    markCommentAsViewed(newFocusedCommentId)
  }

  fun onDownKeyPressed() {
    if (storyLoadingState.value !is StoryLoadingState.Content) return
    val focusedCommentId = focusedCommentId.value ?: return
    val uiComments = uiComments.value
    val focusedCommentIndex = uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex >= uiComments.lastIndex) return
    val newFocusedCommentId = uiComments[focusedCommentIndex + 1].id
    this.focusedCommentId.value = newFocusedCommentId
    scroll.value = adjustScroll(newFocusedCommentId)
    markCommentAsViewed(newFocusedCommentId)
  }

  fun onRightKeyPressed() {
    if (storyLoadingState.value !is StoryLoadingState.Content) return
    val focusedCommentId = focusedCommentId.value ?: return
    val isCollapsed = focusedCommentId in collapsedCommentIds.value
    if (isCollapsed) {
      collapseOrExpandFocusedComment()
      return
    }
    onDownKeyPressed()
  }

  fun onLeftKeyPressed() {
    val state = storyLoadingState.value as? StoryLoadingState.Content ?: return
    val focusedCommentId = focusedCommentId.value ?: return
    val isCollapsed = focusedCommentId in collapsedCommentIds.value
    val focusedComment = state.commentsById[focusedCommentId]!!
    if (!isCollapsed && focusedComment.comments.isNotEmpty()) {
      collapseOrExpandFocusedComment()
      return
    }
    val parentCommentId = focusedComment.parentComment?.id
    if (parentCommentId == null) {
      onUpKeyPressed()
      return
    }
    this.focusedCommentId.value = parentCommentId
    scroll.value = adjustScroll(parentCommentId)
    markCommentAsViewed(parentCommentId)
  }

  private fun collapseOrExpandFocusedComment() {
    val state = storyLoadingState.value as? StoryLoadingState.Content ?: return
    val focusedCommentId = focusedCommentId.value ?: return
    val isCollapsed = focusedCommentId in collapsedCommentIds.value
    collapsedCommentIds.value = if (isCollapsed) {
      collapsedCommentIds.value - focusedCommentId
    } else {
      val focusedComment = state.commentsById[focusedCommentId]!!
      if (focusedComment.comments.isEmpty()) return
      collapsedCommentIds.value + focusedComment.allCollapsibleCommentIds()
    }
    scroll.value = adjustScroll(focusedCommentId)
  }

  private fun adjustScroll(newFocusedCommentId: Comment.Id?): Int {
    if (newFocusedCommentId == null) return 0
    val uiComments = uiComments.value
    val uiStory = uiStory.value ?: return 0
    val screenHeight = screenDimensions.value?.height ?: return 0
    if (newFocusedCommentId == uiComments.first().id) {
      // If the new focused comment is the first one, scroll to the top
      return 0
    }
    var totalHeightUntilFocusedCommentExcluded =
      1 + uiStory.titleLines.size + uiStory.urlLines.size + uiStory.textLines.size + 1
    var totalHeightUntilFocusedCommentIncluded = totalHeightUntilFocusedCommentExcluded
    for (comment in uiComments) {
      totalHeightUntilFocusedCommentIncluded += 1 + comment.textLines.size + 1
      if (comment.id == newFocusedCommentId) break
      totalHeightUntilFocusedCommentExcluded += 1 + comment.textLines.size + 1
    }
    val scroll = scroll.value
    val additionalMargin = (screenHeight * SCROLL_ADDITIONAL_MARGIN_FACTOR).toInt()
    return if (totalHeightUntilFocusedCommentExcluded - additionalMargin < scroll) {
      (totalHeightUntilFocusedCommentExcluded - additionalMargin).coerceAtLeast(0)
    } else {
      if (totalHeightUntilFocusedCommentIncluded - scroll + additionalMargin > screenHeight) {
        (totalHeightUntilFocusedCommentIncluded - screenHeight + additionalMargin)
      } else {
        scroll
      }
    }
  }

  private fun markCommentAsViewed(newFocusedCommentId: Comment.Id) {
    viewModelScope.launch {
      storageRepository.markCommentAsViewed(newFocusedCommentId)
    }
  }
}
