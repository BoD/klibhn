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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
  val screenWidth = screenSize.columns
  // Need to remove 1 because of the cursor which adds a new line
  val screenHeight = screenSize.rows - 1

  Box(
    modifier = Modifier.size(width = screenWidth, height = screenHeight),
  ) {
    val viewModel = viewModel(ApplicationViewModelStoreOwner) { MainViewModel() }
    val state by viewModel.state.collectAsState()

    when (val state = state) {
      is MainViewModel.State.StoryList -> StoryListScreen(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        onSelectStory = { id -> viewModel.selectStory(id) }
      )

      is MainViewModel.State.StoryDetails -> StoryDetailsScreen(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        storyId = state.id,
        onGoBackToStoryList = { viewModel.goBackToStoryList() }
      )
    }
  }
}
