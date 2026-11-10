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

@file:OptIn(ExperimentalTime::class)

package org.jraf.klibhn.internal.client.html

import io.ktor.utils.io.readText
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class HnHtmlParserTest {
  @Test
  fun parseStoryWithText() {
    val html = SystemFileSystem.source(Path("../etc/fixtures/story-with-text.html")).buffered().use { it.readText() }
    val storyWithComments = HnHtmlParser().parseStoryWithComments(html)
    assertEquals(50319510, storyWithComments.id.id)
    assertEquals("2026-10-31T19:56:53Z", storyWithComments.creationDate.toString())
    assertEquals("HackerTube: YouTube built from videos recommended by HN", storyWithComments.title)
    assertEquals("karpathy", storyWithComments.author)
    assertEquals(2867, storyWithComments.score)
    assertEquals(
      """YouTube-style UI over 6,454 videos sourced from 39,834 HN comments since 2007.<p><a href="https:&#x2F;&#x2F;hackertube.org&#x2F;" rel="nofollow">https:&#x2F;&#x2F;hackertube.org&#x2F;</a>""",
      storyWithComments.text,
    )
    assertEquals("https://karpathy.bearblog.dev/hacker-tube/", storyWithComments.url)

    assertEquals(225, storyWithComments.comments.size)
    val comment0 = storyWithComments.comments[0]
    assertEquals(50327005, comment0.id.id)
    assertEquals("2026-11-01T16:17:28Z", comment0.creationDate.toString())
    assertEquals("theptip", comment0.author)
    assertEquals(
      """I think there is a massive opportunity for this kind of feed disaggregation. Used to be the recommender and indexing of content was hard, now I believe we can likely build our own according to our own preferences.<p>Haiku&#x2F;Sonnet (or even Jev) can do a lot of the filtering work to rank and extend an RSS feed list.<p>I think this gives us an opportunity to fully cut the cord from eyeball merchants who want us to maximally engage and therefore polarize their content.<p>The big remaining hook will be the social graph; I am interested to see how far the EU push for mandatory interoperability will go. Another angle I’d support would be mandatory pluggable standards for your own ranking algorithm, but it captures less of the win than full disaggregation.""",
      comment0.text,
    )
    assertEquals(4, comment0.comments.size)
    val comment0_0 = comment0.comments[0]
    assertEquals(50333322, comment0_0.id.id)
    assertEquals("2026-11-02T05:02:16Z", comment0_0.creationDate.toString())
    assertEquals("appplication", comment0_0.author)
    assertEquals(
      """It seems a bit like tragedy of the commons. If we all move towards consuming content this way, who is posting what and where? How is it discovered?<p>One of the great losses of the age of social media is physical community, as people substitute digital presence for in-person interactions. This is the next step with the loss of digital community.""",
      comment0_0.text,
    )
    assertEquals(5, comment0_0.comments.size)
  }

  @Test
  fun parseStoryWithoutText() {
    val html = SystemFileSystem.source(Path("../etc/fixtures/story-without-text.html")).buffered().use { it.readText() }
    val storyWithComments = HnHtmlParser().parseStoryWithComments(html)
    assertNull(storyWithComments.text)
  }

  @Test
  fun parseStoryWithoutUrl() {
    val html = SystemFileSystem.source(Path("../etc/fixtures/story-without-url.html")).buffered().use { it.readText() }
    val storyWithComments = HnHtmlParser().parseStoryWithComments(html)
    assertNull(storyWithComments.url)
  }
}
