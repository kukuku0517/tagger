package com.project.tagger

import android.app.Application
import com.project.tagger.database.AppDatabase
import com.project.tagger.database.PreferenceModel
import com.project.tagger.database.PreferenceModelImpl
import com.project.tagger.gallery.*
import com.project.tagger.login.UpdateUserUC
import com.project.tagger.login.UserRepository
import com.project.tagger.registeredGallery.RegisteredGalleryViewModel
import com.project.tagger.tag.TagViewModel
import com.facebook.stetho.Stetho
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class GlobalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
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
    factory { UpdateUserUC(get()) }

    factory { GalleryViewModel(get(), get()) }

    factory { TagViewModel(get()) }

    factory { RegisteredGalleryViewModel(get()) }

    single<GalleryRepository> { LocalGalleryRepository(get(), get()) }
    single { UserRepository(get(), get()) }
    single<PreferenceModel> { PreferenceModelImpl(get()) }

    factory { AppDatabase.getInstance(get()) }
}