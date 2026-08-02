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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Arrangement
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import org.jraf.hntui.repository.HnRepository
import org.jraf.hntui.ui.ApplicationViewModelStoreOwner
import org.jraf.hntui.util.abbreviate
import org.jraf.klibhn.model.Story

//private val Escape = KeyEvent("Escape")
private val ArrowUp = KeyEvent("ArrowUp")
private val ArrowDown = KeyEvent("ArrowDown")
private val Enter = KeyEvent("Enter")

@Composable
fun StoryListScreen(
  screenWidth: Int,
  screenHeight: Int,
  onSelectStory: (Story.Id) -> Unit,
) {
  val viewModel = viewModel(ApplicationViewModelStoreOwner) { StoryListViewModel(HnRepository.instance) }

  LaunchedEffect(screenHeight) {
    viewModel.setScreenHeight(screenHeight)
  }

  val state by viewModel.state.collectAsState()

  when (val state = state) {
    StoryListViewModel.State.Loading -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          value = "Loading...",
        )
      }
    }

    is StoryListViewModel.State.Error -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          value = "Error: ${state.throwable}",
        )
      }
    }

    is StoryListViewModel.State.Content -> {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .onKeyEvent { keyEvent ->
            when (keyEvent) {
              ArrowUp -> {
                viewModel.focusUp()
              }

              ArrowDown -> {
                viewModel.focusDown()
              }

              Enter -> {
                onSelectStory(state.content[state.focusedIndex].id)
              }

              else -> return@onKeyEvent false
            }
            true
          },
      ) {
        for (story in state.content.drop(state.scroll).take(screenHeight)) {
          val url = story.urlAbbreviated ?: ""
          val indexStr = "${story.index + 1}"
          val title = " " + story.title.abbreviate(screenWidth - indexStr.length - url.length - 2) + " "
          val isFocused = state.focusedIndex == story.index
          Text(
            value = buildAnnotatedString {
              withStyle(
                SpanStyle(
                  textStyle = if (isFocused) {
                    TextStyle.Invert
                  } else {
                    TextStyle.Unspecified
                  },
                ),
              ) {
                withStyle(SpanStyle(color = Color(.5f, .5f, .5f))) {
                  append(indexStr)
                }
                append(title)
                withStyle(SpanStyle(textStyle = TextStyle.Dim + if (isFocused) TextStyle.Invert else TextStyle.Unspecified)) {
                  append(url)
                }
                val padding = screenWidth - indexStr.length - title.length - url.length
                if (padding > 0) {
                  append(" ".repeat(padding))
                }
              }
            },
          )
        }
      }
    }
  }
}
