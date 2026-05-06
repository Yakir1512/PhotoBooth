package com.photobooth.di

import com.photobooth.domain.service.CameraService
import com.photobooth.domain.service.PrinterService
import com.photobooth.domain.service.SharingService
import com.photobooth.impl.AndroidPrinterService
import com.photobooth.impl.AndroidSharingService
import com.photobooth.impl.CameraXService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires domain interfaces to concrete implementations.
 * Dependency Inversion Principle: domain code only sees the interface.
 * To swap camera/printer/sharing implementation, change ONLY this file.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCameraService(impl: CameraXService): CameraService

    @Binds
    @Singleton
    abstract fun bindPrinterService(impl: AndroidPrinterService): PrinterService

    @Binds
    @Singleton
    abstract fun bindSharingService(impl: AndroidSharingService): SharingService
}
