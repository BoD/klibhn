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

package org.jraf.hntui.ui.story.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.jraf.hntui.repository.HnRepository
import org.jraf.klibhn.model.Story

class StoryListViewModel(private val hnRepository: HnRepository) : ViewModel(
  // The default scope uses Dispatchers.Main.immediate, which is not available by default.
  // Use Dispatchers.Default instead.
  viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
  sealed interface State {
    object Loading : State
    data class Error(val throwable: Throwable) : State
    data class Content(
      val stories: List<UiStoryOverview>,
      val focusedIndex: Int,
      val scroll: Int,
    ) : State
  }

  data class UiStoryOverview(
    val id: Story.Id,
    val index: Int,
    val title: String,
    val urlAbbreviated: String?,
  )

  private var screenHeight = 0
  private val requestedStoryCount = MutableStateFlow<Int?>(null)
  private val focusedIndex = MutableStateFlow(0)
  private val scroll = MutableStateFlow(0)

  private var storyIdsCount = 0
  private val storyIds: Flow<Result<List<Story.Id>>> = flow {
    emit(
      hnRepository.getBestStoryIds().also {
        storyIdsCount = it.getOrNull()?.size ?: 0
      },
    )
  }

  private var storyOverViews = emptyList<Story.Overview>()

  val state: StateFlow<State> = combine(
    storyIds,
    requestedStoryCount,
    focusedIndex,
    scroll,
  ) { storyIds, requestedStoryCount, focusedIndex, scroll ->
    if (requestedStoryCount == null) {
      return@combine State.Loading
    }
    val storyIds = storyIds.getOrElse {
      return@combine State.Error(it)
    }
    val storyIdsToLoad = storyIds.take(requestedStoryCount).drop(storyOverViews.size)
    if (storyIdsToLoad.isNotEmpty()) {
      val storyOverviewsPage = hnRepository.getStoryOverviews(storyIdsToLoad).getOrElse {
        return@combine State.Error(it)
      }
      this.storyOverViews += storyOverviewsPage
    }

    val uiStories = this.storyOverViews.mapIndexed { index, overview ->
      UiStoryOverview(
        id = overview.id,
        index = index,
        title = overview.title,
        urlAbbreviated = overview.url?.abbreviatedUrl(),
      )
    }
    State.Content(uiStories, focusedIndex, scroll)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = State.Loading,
  )

  fun setScreenHeight(screenHeight: Int) {
    if (screenHeight == this.screenHeight) return
    this.screenHeight = screenHeight
    requestStoryCount(screenHeight)
    if (focusedIndex.value < scroll.value) {
      scroll.value = focusedIndex.value
    } else if (focusedIndex.value >= scroll.value + screenHeight) {
      scroll.value = focusedIndex.value - screenHeight + 1
    }
  }

  private fun requestStoryCount(requestedStoryCount: Int) {
    // No need to reload stories if the requested count gets smaller than what we currently have
    // TODO: we could be smarter and only fetch/append the stories needed due to the new count
    if (requestedStoryCount > (this.requestedStoryCount.value ?: 0)) {
      // Actually request 1.5x more than hinted
      this.requestedStoryCount.value = (requestedStoryCount * 1.5).toInt()
    }
  }

  fun focusUp() {
    if (state.value !is State.Content) return
    val newFocusedIndex = focusedIndex.value - 1
    if (newFocusedIndex < 0) return
    focusedIndex.value = newFocusedIndex

    if (newFocusedIndex < scroll.value) {
      scroll.value--
    }
  }

  fun focusDown() {
    if (state.value !is State.Content) return
    val newFocusIndex = focusedIndex.value + 1
    if (newFocusIndex > storyIdsCount - 1) return
    if (newFocusIndex >= (requestedStoryCount.value ?: 0)) {
      requestStoryCount(newFocusIndex + 1)
    }
    focusedIndex.value = newFocusIndex

    if (newFocusIndex >= scroll.value + screenHeight) {
      scroll.value++
    }
  }
}

private fun String.abbreviatedUrl(): String {
  return removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore('/')
    // xyz.com -> xyz, but xxx.xyz.com -> xxx.xyz.com
    .replace(Regex("^([^/.]+)\\.com($|/.*)"), "$1$2")
}
