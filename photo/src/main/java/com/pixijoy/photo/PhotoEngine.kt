package com.pixijoy.photo

/**
 * Facade for stills. GLES/AGSL implementation lands in later PRs.
 * UI talks only to this type (Strategy + Command graph).
 */
interface PhotoEngine {
    fun name(): String = "photo-engine-stub"
}

class StubPhotoEngine : PhotoEngine
