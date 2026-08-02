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

import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.buildAnnotatedString

fun String.abbreviate(maxLength: Int): String {
  return if (length <= maxLength) {
    this
  } else {
    val abbreviated = take(maxLength - 1)
    val spacesSuffix = abbreviated.takeLastWhile { it.isWhitespace() }
    abbreviated.trim() + "…" + spacesSuffix
  }
}

fun AnnotatedString.abbreviate(maxLength: Int): AnnotatedString {
  return if (length <= maxLength) {
    this
  } else {
    buildAnnotatedString {
      val abbreviated = take(maxLength - 1)
      val spacesSuffix = abbreviated.takeLastWhile { it.isWhitespace() }
      append(abbreviated.trim())
      append("…")
      append(spacesSuffix)
    }
  }
}

private fun String.split(maxWidth: Int, firstLineMaxWidth: Int): List<String> {
  val lines = mutableListOf<String>()
  var currentLine = ""
  var currentMaxWidth = firstLineMaxWidth
  for (c in this) {
    if (currentLine.length + 1 > currentMaxWidth) {
      lines += currentLine
      currentLine = c.toString()
      currentMaxWidth = maxWidth
    } else {
      currentLine += c
    }
  }
  lines += currentLine
  return lines
}

fun List<String>.wrapped(maxLength: Int): List<String> = flatMap { it.wrapped(maxLength) }

fun String.wrapped(maxWidth: Int): List<String> {
  if (length <= maxWidth) {
    return listOf(this)
  }
  val lines = mutableListOf<String>()
  val words = split(" ").toMutableList()
  var currentLine = ""
  while (words.isNotEmpty()) {
    val word = words.removeAt(0)
    val newLine = if (currentLine.isEmpty()) {
      word
    } else {
      "$currentLine $word"
    }
    if (newLine.length > maxWidth) {
      if (word.length > maxWidth) {
        words.addAll(0, word.split(maxWidth, maxWidth - currentLine.length - 1))
        continue
      } else {
        lines.add(currentLine)
        currentLine = word
      }
    } else {
      currentLine = newLine
    }
  }
  if (currentLine.isNotEmpty()) {
    lines.add(currentLine)
  }
  return lines
}

fun String.htmlToText(): String {
  return replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&#x27;", "'")
    .replace("&#x2F;", "/")
    .replace("&#x60;", "`")
    .replace("&#x3C;", "<")
    .replace("&#x3E;", ">")
    .replace(Regex("<br\\s*/?>"), "\n")
    .replace(Regex("<p\\s*/?>"), "\n\n")
    .replace(Regex("<[^>]+>"), "")
}
