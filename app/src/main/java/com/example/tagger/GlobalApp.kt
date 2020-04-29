package com.example.tagger

import android.app.Application
import com.example.tagger.database.AppDatabase
import com.example.tagger.gallery.*
import com.example.tagger.registeredGallery.RegisteredGalleryViewModel
import com.example.tagger.tag.TagViewModel
import com.facebook.stetho.Stetho
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class GlobalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG){
            Stetho.initializeWithDefaults(this)
        }
        startKoin {
            androidContext(this@GlobalApp)
            modules(
                listOf(
                    module
                )
            )
        }
    }
}

val module = module {
    factory { GetPhotosFromGalleryUC(get()) }
    factory { RegisterPhotoUc(get()) }
    factory { GetRegisteredPhotosUC(get()) }
    factory { RegisterTagsOnPhotosUC(get()) }


    factory { GalleryViewModel(get(),get()) }

    factory {     TagViewModel(get()) }

    factory { RegisteredGalleryViewModel(get()) }

    single<GalleryRepository>{ LocalGalleryRepository(get(),get()) }

    factory { AppDatabase.getInstance(get()) }
}