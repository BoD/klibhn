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

package org.jraf.hntui.data.storage.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.io.files.Path
import org.jraf.hntui.data.storage.db.hn.HnDatabase

internal actual fun createSqlDriver(dbFile: Path): SqlDriver {
  val conf = DatabaseConfiguration(
    name = dbFile.name,
    version = HnDatabase.Schema.version.toInt(),
    create = { connection -> wrapConnection(connection) { HnDatabase.Schema.create(it) } },
    upgrade = { connection, oldVersion, newVersion ->
      wrapConnection(connection) { HnDatabase.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
    },
    extendedConfig = DatabaseConfiguration.Extended(basePath = dbFile.parent.toString()),
  )

  return NativeSqliteDriver(conf)
}
