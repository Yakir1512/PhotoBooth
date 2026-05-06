package com.photobooth

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and set up the dependency injection component hierarchy.
 */
@HiltAndroidApp
class PhotoBoothApplication : Application()
