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

package org.jraf.hntui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.unit.Constraints

/**
 * Note: can only receive 1 child.
 */
@Composable
fun WithConstraints(
  modifier: Modifier = Modifier,
  content: @Composable (Constraints) -> Unit,
) {
  var currentConstraints by remember { mutableStateOf<Constraints?>(null) }

  Layout(
    modifier = modifier,
    content = {
      currentConstraints?.let { content(it) }
    },
  ) { measurables, constraints ->
    if (currentConstraints != constraints) {
      currentConstraints = constraints
    }

    val child = measurables.singleOrNull()?.measure(constraints)
    layout(
      width = child?.width ?: constraints.minWidth,
      height = child?.height ?: constraints.minHeight,
    ) {
      child?.place(0, 0)
    }
  }
}
