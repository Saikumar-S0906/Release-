package com.pixijoy.studio.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.core.data.StudioPrefs
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.PackFeatureEntry
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.photo.BitmapPhotoEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Activity-scoped studio dependencies. Session screens share this instance. */
@HiltViewModel
class StudioShellViewModel @Inject constructor(
    val repository: ProjectRepository,
    val prefs: StudioPrefs,
    val photoEngine: BitmapPhotoEngine,
    val movieEngine: Media3MovieEngine,
    packEngineHooks: PackEngineHooks,
    @ApplicationContext context: Context
) : ViewModel() {
    init {
        PackFeatureEntry.watchVerifiedModels(context, packEngineHooks)
    }
}
