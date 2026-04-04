package com.geny.app.core.storage

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object StorageModule
// TokenManager and SettingsDataStore are auto-provided via @Inject constructor
