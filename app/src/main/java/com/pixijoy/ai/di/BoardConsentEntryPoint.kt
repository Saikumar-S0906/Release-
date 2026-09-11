package com.pixijoy.ai.di

import com.pixijoy.ai.data.ConsentRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BoardConsentEntryPoint {
    fun consentRepository(): ConsentRepository
}
