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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

class TuiMainCoroutineDispatcher : MainCoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    Dispatchers.Default.dispatch(context, block)
  }

  override val immediate: MainCoroutineDispatcher = this
}


@OptIn(InternalCoroutinesApi::class)
internal class TuiDispatcherFactory : MainDispatcherFactory {
  override val loadPriority: Int = 0

  override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
    return TuiMainCoroutineDispatcher()
  }
}
