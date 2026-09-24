package com.pixijoy.core.data.db

import androidx.room.migration.Migration

/**
 * Baseline schema is version 1. Add named migrations here before changing
 * [StudioDatabase] metadata so exported schemas stay testable.
 */
object StudioMigrations {
    const val CURRENT_VERSION = 1

    val all: Array<Migration> = emptyArray()
}
