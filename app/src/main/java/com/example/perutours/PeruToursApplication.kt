package com.example.perutours

import android.app.Application
import com.example.perutours.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PeruToursApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PeruToursApplication)
            modules(appModule)
        }
    }
}