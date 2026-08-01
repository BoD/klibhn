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

@file:OptIn(ExperimentalCoroutinesApi::class)

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.jraf.klibhn.client.HnClient
import org.jraf.klibhn.model.Story
import org.jraf.klibnanolog.logd

class MainViewModel: ViewModel() {
  sealed interface State {
    object Loading : State
    data class Error(val throwable: Throwable) : State
    data class Content(val content: List<Story.Overview>) : State
  }

  private val viewModelScope2 = CoroutineScope(Dispatchers.Default)

  private val hnClient by lazy {
    HnClient(
      HnClient.Configuration(
        HnClient.Configuration.Http(
          loggingLevel = HnClient.Configuration.Http.HttpLoggingLevel.NONE,
        ),
      ),
    )
  }

  private val screenHeight = MutableStateFlow<Int?>(null)

  val state: StateFlow<State> = screenHeight.flatMapLatest { screenHeight ->
    flow {
      emit(State.Loading)
      if ((screenHeight == null)) {
        return@flow
      }
      val bestStoryIds = hnClient.getBestStoryIds().getOrElse {
        emit(State.Error(it))
        return@flow
      }
      val bestStories = hnClient.getStoryOverviews(bestStoryIds.take(screenHeight)).getOrElse {
        emit(State.Error(it))
        return@flow
      }
      emit(State.Content(bestStories))
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = State.Loading,
  )

  fun setScreenHeight(screenHeight: Int) {
    // No need to reload stories if the height gets smaller
    // TODO: we should be smarter and only fetch/append the stories needed due to the new height
    if (screenHeight > (this.screenHeight.value ?: 0)) {
      this.screenHeight.value = screenHeight
    }
  }
}
