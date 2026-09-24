package com.pixijoy.ai.ui

import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNavigationStateTest {
    @Test
    fun saver_roundTripsOnlyStableSessionIdentity() {
        val session = OpenSession("project-1", "curves", MediaKind.PHOTO)

        val restored = OpenSession.fromSaveable(session.toSaveable())

        assertEquals(session, restored)
        assertEquals(listOf("project-1", "curves", "PHOTO"), session.toSaveable())
    }

    @Test
    fun saver_rejectsMalformedKind() {
        assertEquals(null, OpenSession.fromSaveable(listOf("project-1", "editor", "bad")))
    }

    @Test
    fun resolver_reportsLoadingReadyAndMissingWithoutSnapshotFallback() {
        val session = OpenSession("project-1", "editor", MediaKind.PHOTO)
        val project = project("project-1", MediaKind.PHOTO)

        assertTrue(resolveSessionProject(session, null) is SessionProjectState.Loading)
        val ready = resolveSessionProject(session, listOf(project)) as SessionProjectState.Ready
        assertSame(project, ready.project)
        assertTrue(resolveSessionProject(session, emptyList()) is SessionProjectState.Missing)
        assertTrue(
            resolveSessionProject(session, listOf(project("project-1", MediaKind.MOVIE))) is
                SessionProjectState.Missing
        )
    }

    @Test
    fun continueProject_isExposedOnlyWhileLastProjectExists() {
        val existing = project("project-1", MediaKind.PHOTO)

        assertSame(existing, findContinueProject(listOf(existing), "project-1"))
        assertEquals(null, findContinueProject(listOf(existing), "deleted"))
        assertEquals(null, findContinueProject(listOf(existing), null))
    }

    private fun project(id: String, kind: MediaKind) = Project(
        id = id,
        title = "Project",
        kind = kind,
        createdAt = 1,
        updatedAt = 1,
        coverPath = "cover",
        graph = "",
        media = emptyList()
    )
}
