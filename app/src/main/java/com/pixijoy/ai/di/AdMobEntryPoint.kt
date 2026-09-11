package com.pixijoy.ai.di

import com.pixijoy.ai.ads.AdMobManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdMobEntryPoint {
    fun adMobManager(): AdMobManager
}
