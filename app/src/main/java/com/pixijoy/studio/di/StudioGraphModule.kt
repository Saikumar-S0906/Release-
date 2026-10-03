package com.pixijoy.studio.di

import android.content.Context
import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.core.data.StudioPrefs
import com.pixijoy.ml.MediaPipeVision
import com.pixijoy.ml.PackReadiness
import com.pixijoy.ml.StorePackReadiness
import com.pixijoy.ml.VisionFacade
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.photo.BitmapPhotoEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * One studio graph. The shell and session screens receive these through
 * ViewModels and only navigate or observe. They do not construct repositories
 * or engines. Export workers use [StudioGraph] for the same bindings.
 *
 * [ExportPhotoEngine] is unscoped. Each export gets its own engine and
 * segmenter so `lutDirectory` and MediaPipe calls do not race the session
 * singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudioGraphModule {
    @Provides
    @Singleton
    fun provideProjectRepository(@ApplicationContext context: Context): ProjectRepository =
        ProjectRepository(context)

    @Provides
    @Singleton
    fun provideStudioPrefs(@ApplicationContext context: Context): StudioPrefs =
        StudioPrefs(context)

    @Provides
    @Singleton
    fun provideVision(@ApplicationContext context: Context): VisionFacade =
        MediaPipeVision(context)

    @Provides
    @Singleton
    fun providePhotoEngine(vision: VisionFacade): BitmapPhotoEngine =
        BitmapPhotoEngine(vision)

    @Provides
    @Singleton
    fun provideMovieEngine(): Media3MovieEngine = Media3MovieEngine()

    @Provides
    @Singleton
    fun providePackReadiness(@ApplicationContext context: Context): PackReadiness =
        StorePackReadiness(context)

    @Provides
    @ExportPhotoEngine
    fun provideExportPhotoEngine(@ApplicationContext context: Context): BitmapPhotoEngine =
        BitmapPhotoEngine(MediaPipeVision(context))
}

/** Marks a photo engine that belongs to one export job, not the session shell. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExportPhotoEngine

/** Read-only door into [StudioGraphModule] for workers, which have no ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StudioGraph {
    fun repository(): ProjectRepository

    fun movieEngine(): Media3MovieEngine

    @ExportPhotoEngine
    fun exportPhotoEngine(): BitmapPhotoEngine
}

fun studioGraph(context: Context): StudioGraph =
    EntryPointAccessors.fromApplication(context, StudioGraph::class.java)
