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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.UnderlineStyle
import org.jraf.hntui.data.HnRepository
import org.jraf.hntui.ui.components.Error
import org.jraf.hntui.ui.components.Loading
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiComment
import org.jraf.hntui.ui.story.details.StoryDetailsViewModel.UiState.Content.UiStory
import org.jraf.hntui.util.ArrowDown
import org.jraf.hntui.util.ArrowUp
import org.jraf.hntui.util.Escape
import org.jraf.hntui.util.WithConstraints
import org.jraf.hntui.util.abbreviate
import org.jraf.hntui.util.formatted
import org.jraf.klibhn.model.Story

@Composable
fun StoryDetailsScreen(
  storyId: Story.Id,
  onGoBackToStoryList: () -> Unit,
) {
  val viewModel = viewModel(key = "StoryDetailsViewModel:$storyId") {
    StoryDetailsViewModel(hnRepository = HnRepository.instance, storyId = storyId)
  }
  WithConstraints(modifier = Modifier.fillMaxSize()) { constraints ->
    val availableHeight = constraints.maxHeight
    val availableWidth = constraints.maxWidth
    LaunchedEffect(constraints) {
      viewModel.setScreenDimensions(availableWidth, availableHeight)
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .onKeyEvent { keyEvent ->
          when (keyEvent) {
            Escape -> {
              onGoBackToStoryList()
            }

            ArrowUp -> {
              viewModel.focusUp()
            }

            ArrowDown -> {
              viewModel.focusDown()
            }

            else -> return@onKeyEvent false
          }
          true
        },
    ) {
      val uiState by viewModel.uiState.collectAsState()
      when (val uiState = uiState) {
        UiState.Loading -> {
          Loading()
        }

        is UiState.Error -> {
          Error(uiState.throwable)
        }

        is UiState.Content -> {
          Story(
            content = uiState,
            availableWidth = availableWidth,
            availableHeight = availableHeight,
          )
        }
      }
    }
  }
}

@Composable
private fun Story(
  content: UiState.Content,
  availableWidth: Int,
  availableHeight: Int,
) {
  val story = content.story
  Column(
    modifier = Modifier.fillMaxSize(),
  ) {
    val lines = story.toHeaderAnnotatedString(
      availableWidth = availableWidth,
      commentCount = content.comments.size,
    ) + content.comments.flatMap { comment ->
      comment.toAnnotatedString(availableWidth, isFocused = comment.id == content.focusedCommentId)
    }
    for (line in lines.slice(content.scroll..<(content.scroll + availableHeight).coerceAtMost(lines.lastIndex))) {
      Text(line)
    }
  }
}

private fun UiStory.toHeaderAnnotatedString(availableWidth: Int, commentCount: Int): List<AnnotatedString> {
  return buildList {
    for (titleLine in titleLines) {
      add(
        AnnotatedString(titleLine, SpanStyle(textStyle = TextStyle.Bold)),
      )
    }
    for (urlLine in urlLines) {
      add(
        AnnotatedString(urlLine, SpanStyle(underlineStyle = UnderlineStyle.Straight)),
      )
    }
    add(
      buildAnnotatedString {
        withStyle(SpanStyle(color = Color.Cyan)) {
          append(
            creationDate.formatted(),
          )
        }
        append(' ')
        withStyle(SpanStyle(color = Color.Magenta)) {
          append(author)
        }
        append(' ')
        withStyle(SpanStyle(color = Color.Red)) {
          append("$score points")
        }
        append(' ')
        withStyle(SpanStyle(color = Color.Green)) {
          append("$commentCount comment${if (commentCount == 1) "" else "s"}")
        }
      }.abbreviate(availableWidth),
    )
    for (textLine in textLines) {
      add(
        AnnotatedString.Builder(textLine).toAnnotatedString(),
      )
    }
  }
}

private fun UiComment.toAnnotatedString(availableWidth: Int, isFocused: Boolean): List<AnnotatedString> {
  val indent = "  ".repeat(indent)
  return buildList {
    val creationDate = creationDate.formatted()
    add(
      buildAnnotatedString {
        withStyle(
          SpanStyle(
            background = if (isFocused) {
              Color(.25f, .25f, .25f)
            } else {
              Color.Unspecified
            },
          ),
        ) {
          append(indent)
          withStyle(SpanStyle(color = Color.Cyan)) {
            append(creationDate)
          }
          append(' ')
          withStyle(SpanStyle(color = Color.Magenta)) {
            append(author)
          }

          if (isFocused) {
            val padding = availableWidth - creationDate.length - 1 - author.length - indent.length
            if (padding > 0) {
              append(" ".repeat(padding))
            }
          }
        }
      }.abbreviate(maxLength = availableWidth),
    )

    for (textLine in textLines) {
      add(
        buildAnnotatedString {
          withStyle(
            SpanStyle(
              background = if (isFocused) {
                Color(.25f, .25f, .25f)
              } else {
                Color.Unspecified
              },
            ),
          ) {
            append(indent)
            append(textLine)
            if (isFocused) {
              val padding = availableWidth - textLine.length - indent.length
              if (padding > 0) {
                append(" ".repeat(padding))
              }
            }
          }
        },
      )
    }
  }
}
