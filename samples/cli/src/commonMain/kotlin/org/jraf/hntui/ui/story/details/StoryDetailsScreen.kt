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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.UnderlineStyle
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.jraf.hntui.data.HnRepository
import org.jraf.hntui.ui.ApplicationViewModelStoreOwner
import org.jraf.hntui.ui.components.Error
import org.jraf.hntui.ui.components.Loading
import org.jraf.hntui.util.WithConstraints
import org.jraf.hntui.util.abbreviate
import org.jraf.hntui.util.htmlToText
import org.jraf.hntui.util.wrapped
import org.jraf.klibhn.model.Story

private val Escape = KeyEvent("Escape")

@Composable
fun StoryDetailsScreen(
  storyId: Story.Id,
  onGoBackToStoryList: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .onKeyEvent { keyEvent ->
        when (keyEvent) {
          Escape -> {
            onGoBackToStoryList()
          }

          else -> return@onKeyEvent false
        }
        true
      },
  ) {
    val viewModel = viewModel(
      viewModelStoreOwner = ApplicationViewModelStoreOwner,
      key = "StoryDetailsViewModel:$storyId",
    ) {
      StoryDetailsViewModel(hnRepository = HnRepository.instance, storyId = storyId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val uiState = uiState) {
      StoryDetailsViewModel.UiState.Loading -> {
        Loading()
      }

      is StoryDetailsViewModel.UiState.Error -> {
        Error(uiState.throwable)
      }

      is StoryDetailsViewModel.UiState.Content -> {
          Story(uiState)
      }
    }
  }
}

@Composable
private fun Story(content: StoryDetailsViewModel.UiState.Content) {
  val story = content.story
  Column(
    modifier = Modifier.fillMaxSize(),
  ) {
    StoryHeader(story)
//          for (comment in story.comments.take(3)) {
//            Comment (comment, screenWidth, 1)
//          }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawBehind {
          for (i in 0..<height - 1) {
            drawText(row = i, column = 0, string = "$i")
          }
          drawText(row = height - 1, column = 0, "End")
        },
    ) {
      Spacer(modifier = Modifier.fillMaxSize())
    }
  }
}

@Composable
@OptIn(FormatStringsInDatetimeFormats::class)
private fun StoryHeader(story: Story.WithComments) {
  WithConstraints(modifier = Modifier.fillMaxWidth()) { constraints ->
    val availableWidth = constraints.maxWidth
    Column(modifier = Modifier.fillMaxWidth()) {
      val titleLines = story.title.wrapped(availableWidth)
      for (titleLine in titleLines) {
        Text(
          AnnotatedString(titleLine, SpanStyle(textStyle = TextStyle.Bold)),
        )
      }
      story.url?.let {
        val urlLines = it.wrapped(availableWidth)
        for (urlLine in urlLines) {
          Text(
            AnnotatedString(urlLine, SpanStyle(underlineStyle = UnderlineStyle.Straight)),
          )
        }
      }
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(color = Color.Red)) {
            append("${story.score} points")
          }
          append(' ')
          withStyle(SpanStyle(color = Color.Magenta)) {
            append(story.author)
          }
          append(' ')
          withStyle(SpanStyle(color = Color.Cyan)) {
            append(
              story.creationDate.toLocalDateTime(TimeZone.currentSystemDefault()).format(
                LocalDateTime.Format {
                  byUnicodePattern("uuuu-MM-dd HH:mm")
                },
              ),
            )
          }
          append(' ')
          withStyle(SpanStyle(color = Color.Green)) {
            val commentCount = story.comments.size
            append("$commentCount comment${if (commentCount == 1) "" else "s"}")
          }
        }.abbreviate(availableWidth),
      )
      story.text?.let {
        val textLines = it.htmlToText().split("\n").wrapped(availableWidth)
        for (textLine in textLines) {
          Text(textLine)
        }
      }
    }
  }
}
