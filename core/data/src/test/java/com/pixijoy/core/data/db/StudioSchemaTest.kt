package com.pixijoy.core.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StudioSchemaTest {
    @Test
    fun currentVersion_isExportedBaseline() {
        assertEquals(1, StudioMigrations.CURRENT_VERSION)
        assertEquals(0, StudioMigrations.all.size)
    }

    @Test
    fun exportedSchema_listsProjectAndMediaTables() {
        val schema = File("schemas/com.pixijoy.core.data.db.StudioDatabase/1.json")
        assertTrue(
            "Room schema export missing at ${schema.absolutePath}",
            schema.exists()
        )
        val body = schema.readText()
        assertTrue(body.contains("\"tableName\": \"projects\""))
        assertTrue(body.contains("\"tableName\": \"media_refs\""))
        assertTrue(body.contains("\"columnName\": \"graph\""))
        assertTrue(body.contains("\"columnName\": \"projectId\""))
    }

    @Test
    fun entities_coverLocalProjectMetadata() {
        val projectFields = ProjectEntity::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(projectFields.containsAll(listOf("id", "title", "kind", "graph", "coverPath")))
        val mediaFields = MediaRefEntity::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(mediaFields.containsAll(listOf("id", "projectId", "path", "mime")))
    }
}
