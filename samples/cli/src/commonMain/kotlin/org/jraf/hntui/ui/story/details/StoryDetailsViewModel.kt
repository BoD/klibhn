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
import org.jraf.hntui.util.Dimensions
import org.jraf.hntui.util.TuiViewModel
import org.jraf.hntui.util.htmlWrapped
import org.jraf.hntui.util.wrapped
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val MAX_COMMENT_WIDTH = 120
private const val SCROLL_ADDITIONAL_MARGIN = 10

class StoryDetailsViewModel(
  private val hnRepository: HnRepository,
  private val storyId: Story.Id,
) : TuiViewModel() {
  companion object {
    const val INDENT_WIDTH = 2
  }

  sealed interface InternalState {
    object Loading : InternalState
    data class Error(val throwable: Throwable) : InternalState
    data class Content(
      val screenDimensions: Dimensions,
      val story: Story.WithComments,
      val commentsById: Map<Comment.Id, Comment>,
      val totalCommentCount: Int,
      val uiStory: UiStory,
      val uiComments: List<UiComment>,
      val focusedCommentId: Comment.Id?,
      val scroll: Int,
      val collapsedCommentIds: Set<Comment.Id>,
      val readCommentIds: Set<Comment.Id>,
    ) : InternalState
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
        val isRead: Boolean,
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
          totalCommentCount = internalState.totalCommentCount,
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

  fun setScreenDimensions(dimensions: Dimensions) {
    when (internalState.value) {
      is InternalState.Error -> {
        return
      }

      is InternalState.Content -> {
        val state = internalState.value as InternalState.Content
        if (state.screenDimensions == dimensions) {
          return
        }
        internalState.value = state.copy(
          screenDimensions = dimensions,
          uiStory = state.story.toUiStory(dimensions.width),
          uiComments = state.story.toUiComments(
            maxWidth = dimensions.width,
            collapsedCommentIds = state.collapsedCommentIds,
            readCommentIds = state.readCommentIds,
          ),
          scroll = adjustScroll(state, state.focusedCommentId),
        )

      }

      is InternalState.Loading -> {
        viewModelScope.launch {
          val story = hnRepository.getStoryWithComments(storyId).getOrElse { e ->
            internalState.value = InternalState.Error(e)
            return@launch
          }
          val readCommentIds = story.comments.firstOrNull()?.id?.let { setOf(it) } ?: emptySet()
          // Collapse all comments by default
          val collapsedCommentIds = story.allCollapsibleCommentIds()
          internalState.value = InternalState.Content(
            screenDimensions = dimensions,
            story = story,
            commentsById = story.commentsById(),
            totalCommentCount = story.totalCommentCount(),
            uiStory = story.toUiStory(dimensions.width),
            uiComments = story.toUiComments(
              maxWidth = dimensions.width,
              collapsedCommentIds = collapsedCommentIds,
              readCommentIds = readCommentIds,
            ),
            focusedCommentId = story.comments.firstOrNull()?.id,
            scroll = 0,
            collapsedCommentIds = collapsedCommentIds,
            readCommentIds = readCommentIds,
          )
        }
      }
    }
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
    readCommentIds: Set<Comment.Id>,
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
            isRead = comment.id in readCommentIds,
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

  private fun Story.WithComments.totalCommentCount(): Int {
    var count = 0
    fun countComments(comments: List<Comment>) {
      for (comment in comments) {
        count++
        countComments(comment.comments)
      }
    }
    countComments(comments)
    return count
  }


  fun onUpKeyPressed() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val focusedCommentIndex = state.uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex <= 0) return
    val newFocusedCommentId = state.uiComments[focusedCommentIndex - 1].id
    val newScroll = adjustScroll(state, newFocusedCommentId)
    val readCommentIds = state.readCommentIds + newFocusedCommentId
    internalState.value = state.copy(
      uiComments = state.story.toUiComments(
        maxWidth = state.screenDimensions.width,
        collapsedCommentIds = state.collapsedCommentIds,
        readCommentIds = readCommentIds,
      ),
      focusedCommentId = newFocusedCommentId,
      scroll = newScroll,
      readCommentIds = readCommentIds,
    )
  }

  fun onDownKeyPressed() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val focusedCommentIndex = state.uiComments.indexOfFirst { it.id == focusedCommentId }
    if (focusedCommentIndex >= state.uiComments.lastIndex) return
    val newFocusedCommentId = state.uiComments[focusedCommentIndex + 1].id
    val newScroll = adjustScroll(state, newFocusedCommentId)
    val readCommentIds = state.readCommentIds + newFocusedCommentId
    internalState.value = state.copy(
      uiComments = state.story.toUiComments(
        maxWidth = state.screenDimensions.width,
        collapsedCommentIds = state.collapsedCommentIds,
        readCommentIds = readCommentIds,
      ),
      focusedCommentId = newFocusedCommentId,
      scroll = newScroll,
      readCommentIds = readCommentIds,
    )
  }

  fun onRightKeyPressed() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val isCollapsed = focusedCommentId in state.collapsedCommentIds
    if (isCollapsed) {
      collapseOrExpandFocusedComment()
      return
    }
    onDownKeyPressed()
  }

  fun onLeftKeyPressed() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val isCollapsed = focusedCommentId in state.collapsedCommentIds
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
    val newScroll = adjustScroll(state, parentCommentId)
    val readCommentIds = state.readCommentIds + parentCommentId
    internalState.value = state.copy(
      uiComments = state.story.toUiComments(
        maxWidth = state.screenDimensions.width,
        collapsedCommentIds = state.collapsedCommentIds,
        readCommentIds = readCommentIds,
      ),
      focusedCommentId = parentCommentId,
      scroll = newScroll,
      readCommentIds = readCommentIds,
    )
  }

  private fun collapseOrExpandFocusedComment() {
    val state = internalState.value as? InternalState.Content ?: return
    val focusedCommentId = state.focusedCommentId ?: return
    val isCollapsed = focusedCommentId in state.collapsedCommentIds
    val newCollapsedCommentIds = if (isCollapsed) {
      state.collapsedCommentIds - focusedCommentId
    } else {
      val focusedComment = state.commentsById[focusedCommentId]!!
      if (focusedComment.comments.isEmpty()) return
      state.collapsedCommentIds + focusedComment.allCollapsibleCommentIds()
    }
    internalState.value = state.copy(
      uiComments = state.story.toUiComments(
        maxWidth = state.screenDimensions.width,
        collapsedCommentIds = newCollapsedCommentIds,
        readCommentIds = state.readCommentIds,
      ),
      collapsedCommentIds = newCollapsedCommentIds,
      scroll = adjustScroll(state, focusedCommentId),
    )
  }

  private fun adjustScroll(
    state: InternalState.Content,
    newFocusedCommentId: Comment.Id?,
  ): Int {
    if (newFocusedCommentId == null) return 0
    if (newFocusedCommentId == state.uiComments.first().id) {
      // If the new focused comment is the first one, scroll to the top
      return 0
    }
    var totalHeightUntilFocusedCommentExcluded =
      1 + state.uiStory.titleLines.size + state.uiStory.urlLines.size + state.uiStory.textLines.size + 1
    var totalHeightUntilFocusedCommentIncluded = totalHeightUntilFocusedCommentExcluded
    for (comment in state.uiComments) {
      totalHeightUntilFocusedCommentIncluded += 1 + comment.textLines.size + 1
      if (comment.id == newFocusedCommentId) break
      totalHeightUntilFocusedCommentExcluded += 1 + comment.textLines.size + 1
    }
    val scroll = state.scroll
    return if (totalHeightUntilFocusedCommentExcluded - SCROLL_ADDITIONAL_MARGIN < scroll) {
      (totalHeightUntilFocusedCommentExcluded - SCROLL_ADDITIONAL_MARGIN).coerceAtLeast(0)
    } else {
      val screenHeight = state.screenDimensions.height
      if (totalHeightUntilFocusedCommentIncluded - scroll + SCROLL_ADDITIONAL_MARGIN > screenHeight) {
        (totalHeightUntilFocusedCommentIncluded - screenHeight + SCROLL_ADDITIONAL_MARGIN)
      } else {
        scroll
      }
    }
  }
}
