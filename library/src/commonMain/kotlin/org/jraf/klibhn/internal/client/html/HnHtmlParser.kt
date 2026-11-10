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

import org.jraf.klibhn.internal.model.CommentImpl
import org.jraf.klibhn.internal.model.StoryImpl
import org.jraf.klibhn.model.Comment
import org.jraf.klibhn.model.Story
import org.jraf.klibregexdsl.AnyCharacter
import org.jraf.klibregexdsl.Characters
import org.jraf.klibregexdsl.Digit
import org.jraf.klibregexdsl.Group
import org.jraf.klibregexdsl.QuantifierType
import org.jraf.klibregexdsl.Sequence
import org.jraf.klibregexdsl.oneOrMoreTimes
import org.jraf.klibregexdsl.zeroOrMoreTimes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class HnHtmlParser {
  private val idRegex = Sequence(
    Characters("""<link rel="canonical" href="https://news.ycombinator.com/item?id="""),
    Group(Digit.oneOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters("\""),
  )
    .toRegex()

  private val creationDateRegex = Sequence(
    Characters("""<span class="age" title=""""),
    Group(AnyCharacter.oneOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters("\""),
  )
    .toRegex()

  private val titleRegex = Sequence(
    Characters("""<title>"""),
    Group(AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters(""" | Hacker News"""),
  )
    .toRegex()

  private val authorRegex = Sequence(
    Characters("""user?id="""),
    Group(AnyCharacter.oneOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters("\""),
  )
    .toRegex()

  private val scoreRegex = Sequence(
    Characters("""<span class="score" id="score_"""),
    Digit.oneOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("""">"""),
    Group(Digit.oneOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters(""" points"""),
  )
    .toRegex()

  private val topTextRegex = Sequence(
    Characters("""<div class="toptext""""),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters(""">"""),
    Group(AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters("""</div>"""),
  )
    .toRegex()

  private val urlRegex = Sequence(
    Characters("""<span class="titleline"><a href=""""),
    Group(AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT)),
    Characters("\""),
  )
    .toRegex()

  // <tr><td class="ind" indent="0"><img src="s.gif" height="1" width="0"></td><td valign="top" class="votelinks"><center><a id='up_50327005' href='vote?id=50327005&amp;how=up&amp;goto=item%3Fid%3D50319510'><div class='votearrow' title='upvote'></div></a></center></td><td class="default"><div style="margin-top:2px; margin-bottom:-10px;"><span class="comhead"><a href="user?id=theptip" class="hnuser">theptip</a> <span class="age" title="2026-11-01T16:17:28"><a href="item?id=50327005">2 days ago</a></span> <span id="unv_50327005"></span><span class="navs"> | <a href="#50320482" class="clicky" aria-hidden="true">next</a> <a class="togg clicky" id="50327005" n="21" href="javascript:void(0)">[–]</a><span class="onstory"></span></span></span></div><br>
  // <div class="comment"><div class="commtext c00">I think there is a massive opportunity for this kind of feed disaggregation. Used to be the recommender and indexing of content was hard, now I believe we can likely build our own according to our own preferences.<p>Haiku&#x2F;Sonnet (or even Jev) can do a lot of the filtering work to rank and extend an RSS feed list.<p>I think this gives us an opportunity to fully cut the cord from eyeball merchants who want us to maximally engage and therefore polarize their content.<p>The big remaining hook will be the social graph; I am interested to see how far the EU push for mandatory interoperability will go. Another angle I’d support would be mandatory pluggable standards for your own ranking algorithm, but it captures less of the win than full disaggregation.</div><div class="reply"><p><font size="1"><u><a href="reply?id=50327005&amp;goto=item%3Fid%3D50319510%2350327005" rel="nofollow">reply</a></u></font></div></div></td></tr>
  private val commentRegex = Sequence(
    Characters("""<tr><td class="ind" indent=""""),
    Group(Digit.oneOrMoreTimes(QuantifierType.RELUCTANT)), // Indent
    Characters("\""),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("""href='vote?id="""),
    Group(Digit.oneOrMoreTimes(QuantifierType.RELUCTANT)), // Comment ID
    Characters("""&"""),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("""href="user?id="""),
    Group(AnyCharacter.oneOrMoreTimes(QuantifierType.RELUCTANT)), // Author
    Characters("\""),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("""<span class="age" title=""""),
    Group(AnyCharacter.oneOrMoreTimes(QuantifierType.RELUCTANT)), // Creation date
    Characters("\""),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("\n"),
    AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT),
    Characters("""<div class="commtext c00">"""),
    Group(AnyCharacter.zeroOrMoreTimes(QuantifierType.RELUCTANT)), // Text
    Characters("""</div>"""),
  )
    .toRegex()

  fun parseStoryWithComments(html: String): Story.WithComments {
    val title = parseTitle(html)
    val id = parseId(html)
    val creationDate = parseCreationDate(html)
    val author = parseAuthor(html)
    val score = parseScore(html)
    val text = parseText(html).ifEmpty { null }
    val url = parseUrl(html).takeIf { it.startsWith("http") }
    val commentsWithIndents = parseComments(html)
    val comments = structureComments(commentsWithIndents)

    return StoryImpl.WithComments(
      id = Story.Id(id),
      creationDate = Instant.parse(creationDate + "Z"),
      title = title,
      author = author,
      score = score,
      text = text,
      url = url,
      comments = comments,
    )
  }

  private fun parseId(html: String): Long {
    return idRegex.find(html)!!.groupValues[1].toLong()
  }

  private fun parseCreationDate(html: String): String {
    return creationDateRegex.find(html)!!.groupValues[1]
  }

  private fun parseTitle(html: String): String {
    return titleRegex.find(html)!!.groupValues[1]
  }

  private fun parseAuthor(html: String): String {
    return authorRegex.find(html)!!.groupValues[1]
  }

  private fun parseScore(html: String): Int {
    return scoreRegex.find(html)!!.groupValues[1].toInt()
  }

  private fun parseText(html: String): String {
    return topTextRegex.find(html)!!.groupValues[1]
  }

  private fun parseUrl(html: String): String {
    return urlRegex.find(html)!!.groupValues[1]
  }

  private fun parseComments(html: String): List<Pair<Comment, Int>> {
    return commentRegex.findAll(html).map { matchResult ->
      val indent = matchResult.groupValues[1].toInt()
      val id = matchResult.groupValues[2].toLong()
      val author = matchResult.groupValues[3]
      val creationDate = Instant.parse(matchResult.groupValues[4] + "Z")
      val text = matchResult.groupValues[5]
      CommentImpl(
        id = Comment.Id(id),
        creationDate = creationDate,
        author = author,
        text = text,
        comments = emptyList(),
      ) to indent
    }.toList()
  }

  private fun structureComments(commentsWithIndents: List<Pair<Comment, Int>>): List<Comment> {
    val comments = mutableListOf<Comment>()
    val stack = mutableListOf<Pair<Comment, Int>>()
    for ((comment, indent) in commentsWithIndents) {
      while (stack.isNotEmpty() && stack.last().second >= indent) {
        stack.removeAt(stack.lastIndex)
      }
      if (stack.isEmpty()) {
        comments.add(comment)
      } else {
        val parentComment = stack.last().first as CommentImpl
        parentComment.comments += comment
      }
      stack.add(comment to indent)
    }
    return comments
  }
}
