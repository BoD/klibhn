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

package org.jraf.hntui.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import org.jraf.hntui.util.WithConstraints
import org.jraf.hntui.util.abbreviate

@Composable
fun StatusBar(text: String) {
  WithConstraints(
    Modifier
      .fillMaxWidth()
      .height(1),
  ) { constraints ->
    Text(
      value = buildAnnotatedString {
        withStyle(
          SpanStyle(
            textStyle = TextStyle.Invert,
          ),
        ) {
          append(text)
          val padding = constraints.maxWidth - text.length
          if (padding > 0) {
            append(" ".repeat(padding))
          }
        }
      }.abbreviate(constraints.maxWidth),
    )
  }
}
