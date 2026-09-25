package com.pixijoy.ai.di

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
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * One studio graph. Screens receive these through ViewModels; they do not
 * construct repositories or engines themselves.
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
}
