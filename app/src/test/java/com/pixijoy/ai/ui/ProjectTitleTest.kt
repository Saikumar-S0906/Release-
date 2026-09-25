package com.pixijoy.ai.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectTitleTest {
    @Test
    fun sourceName_stripsExtensionAndKeepsWords() {
        assertEquals("IMG_2044", ProjectTitle.fromSourceName("IMG_2044.JPG"))
        assertEquals("Beach sunset", ProjectTitle.fromSourceName("  Beach   sunset.heic "))
    }

    @Test
    fun sourceName_rejectsGenericAndCameraStamps() {
        assertNull(ProjectTitle.fromSourceName("Sample"))
        assertNull(ProjectTitle.fromSourceName("photo.jpg"))
        assertNull(ProjectTitle.fromSourceName("capture-172000.jpg"))
        assertNull(ProjectTitle.fromSourceName("incoming"))
        assertNull(ProjectTitle.fromSourceName("   "))
        assertNull(ProjectTitle.fromSourceName(null))
    }

    @Test
    fun sourceName_dropsUnsafeCharactersAndCapsLength() {
        assertEquals("Trip notes", ProjectTitle.fromSourceName("Trip/notes:.jpg"))
        val long = "a".repeat(80) + ".png"
        assertEquals(48, ProjectTitle.fromSourceName(long)!!.length)
    }

    @Test
    fun resolve_prefersSourceThenDatedKind() {
        assertEquals("IMG_9", ProjectTitle.resolve("IMG_9.dng", "Photo", "Sep 25"))
        assertEquals("Photo · Sep 25", ProjectTitle.resolve("capture-9.jpg", "Photo", "Sep 25"))
        assertEquals("Sample · Sep 25", ProjectTitle.resolve(null, "Sample", "Sep 25"))
        assertEquals("Movie", ProjectTitle.dated("Movie", ""))
    }
}
