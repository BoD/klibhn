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

package org.jraf.hntui.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import org.jraf.hntui.ui.ApplicationViewModelStoreOwner
import org.jraf.hntui.ui.story.details.StoryDetailsScreen
import org.jraf.hntui.ui.story.list.StoryListScreen

@Composable
fun MainScreen() {
  val screenSize = LocalTerminalState.current.size
  Box(
    modifier = Modifier.size(
      width = screenSize.columns,
      height = screenSize.rows - 1, // Need to remove 1 because of the cursor which adds a new line
    ),
  ) {
    val viewModel = viewModel(ApplicationViewModelStoreOwner) { MainViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = uiState) {
      is MainViewModel.UiState.StoryList -> StoryListScreen(
        onSelectStory = { id -> viewModel.selectStory(id) },
      )

      is MainViewModel.UiState.StoryDetails -> StoryDetailsScreen(
        storyId = uiState.id,
        onGoBackToStoryList = { viewModel.goBackToStoryList() },
      )
    }
  }
}
