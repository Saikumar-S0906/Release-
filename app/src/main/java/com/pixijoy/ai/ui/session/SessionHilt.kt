package com.pixijoy.ai.ui.session

import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.movie.MultiTrackTimeline
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Hilt entry for [PhotoSessionViewModel]. Tests keep using the base constructor
 * with an explicit [GraphPersistence]; production persistence is the project repo.
 */
@HiltViewModel(assistedFactory = HiltPhotoSessionViewModel.Factory::class)
class HiltPhotoSessionViewModel @AssistedInject constructor(
    @Assisted("initialGraph") initialGraph: String,
    @Assisted("projectId") projectId: String,
    @Assisted("toolKey") toolKey: String,
    repository: ProjectRepository
) : PhotoSessionViewModel(
    initialGraph = initialGraph,
    persistence = GraphPersistence { graph -> repository.saveGraph(projectId, graph) },
    toolKey = toolKey
) {
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("initialGraph") initialGraph: String,
            @Assisted("projectId") projectId: String,
            @Assisted("toolKey") toolKey: String
        ): HiltPhotoSessionViewModel
    }
}

@HiltViewModel(assistedFactory = HiltMovieSessionViewModel.Factory::class)
class HiltMovieSessionViewModel @AssistedInject constructor(
    @Assisted initialTimeline: MultiTrackTimeline,
    @Assisted projectId: String,
    repository: ProjectRepository
) : MovieSessionViewModel(
    initialTimeline = initialTimeline,
    persistence = GraphPersistence { graph -> repository.saveGraph(projectId, graph) }
) {
    @AssistedFactory
    interface Factory {
        fun create(
            initialTimeline: MultiTrackTimeline,
            projectId: String
        ): HiltMovieSessionViewModel
    }
}
