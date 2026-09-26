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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.jraf.hntui.data.HnRepository
import org.jraf.klibhn.model.Story
import kotlin.time.Duration.Companion.seconds

class StoryDetailsViewModel(
  private val hnRepository: HnRepository,
  private val storyId: Story.Id,
) : ViewModel(
  // The default scope uses Dispatchers.Main.immediate, which is not available by default.
  // Use Dispatchers.Default instead.
  viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
  sealed interface UiState {
    object Loading : UiState
    data class Error(val throwable: Throwable) : UiState
    data class Content(
      val story: Story.WithComments,
    ) : UiState
  }

  val uiState: StateFlow<UiState> = flow {
    hnRepository.getStoryWithComments(storyId).fold(
      onSuccess = { storyWithComments ->
        emit(UiState.Content(storyWithComments))
      },
      onFailure = { throwable ->
        emit(UiState.Error(throwable))
      },
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5.seconds),
    initialValue = UiState.Loading,
  )
}
