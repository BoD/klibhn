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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Arrangement
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import kotlinx.coroutines.awaitCancellation

// Normally we'd clear the store when we're finished with Mosaic.
// In practice however, Mosaic terminates when the whole app terminates, so a process lifetime is fine.
object ApplicationViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore: ViewModelStore = ViewModelStore()
}

fun main(av: Array<String>) {
  runMosaicBlocking {
    val viewModel = viewModel(ApplicationViewModelStoreOwner) { MainViewModel() }

    val screenSize = LocalTerminalState.current.size
    val screenWidth = screenSize.columns
    // Need to remove 1 because of the cursor which adds a new line
    val screenHeight = screenSize.rows - 1

    LaunchedEffect(screenHeight) {
      viewModel.requestStoryCount(screenHeight)
    }

    Box(
      modifier = Modifier.size(width = screenWidth, height = screenHeight),
    ) {
      val state by viewModel.state.collectAsState()

      when (val state = state) {
        MainViewModel.State.Loading -> {
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

        is MainViewModel.State.Error -> {
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

        is MainViewModel.State.Content -> {
          var windowIndex by remember { mutableStateOf(0) }
          Column(
            modifier = Modifier
              .fillMaxSize()
              .onKeyEvent { keyEvent ->
                when (keyEvent) {
                  ArrowUp -> {
                    val focusIndexChanged = viewModel.focusUp()
                    if (focusIndexChanged && (state.focusedIndex - 1).coerceAtLeast(0) < windowIndex) {
                      windowIndex--
                    }
                  }
                  ArrowDown -> {
                    val focusIndexChanged = viewModel.focusDown()
                    if (focusIndexChanged && state.focusedIndex + 1 >= windowIndex + screenHeight) {
                      windowIndex++
                    }
                  }

                  else -> return@onKeyEvent false
                }
                true
              },
          ) {
            for (story in state.content.drop(windowIndex).take(screenHeight)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
              ) {
                val url = story.urlAbbreviated?.let { " $it" } ?: ""
                val indexStr = "${story.index} "
                val title = story.title.abbreviate(screenWidth - indexStr.length - url.length)
                Text(
                  value = buildAnnotatedString {
                    withStyle(
                      SpanStyle(
                        textStyle = if (state.focusedIndex == story.index) {
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
                      withStyle(SpanStyle(color = Color.Red)) {
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

      LaunchedEffect(Unit) {
        awaitCancellation()
      }
    }
  }
}

private fun String.abbreviate(maxLength: Int): String {
  return if (length <= maxLength) {
    this
  } else {
    take(maxLength - 1) + "…"
  }
}

private val ArrowUp = KeyEvent("ArrowUp")
private val ArrowDown = KeyEvent("ArrowDown")
