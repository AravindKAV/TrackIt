package com.upipulse.di

import com.upipulse.service.parser.UpiDetectionParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    @Provides
    @Singleton
    fun provideUpiDetectionParser(): UpiDetectionParser = UpiDetectionParser()
}
