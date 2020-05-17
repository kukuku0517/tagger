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
import com.google.android.gms.ads.MobileAds
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.SignOutUC
import com.project.tagger.my.MyViewModel
import com.project.tagger.my.RepoSearchViewModel
import com.project.tagger.registeredGallery.RegisteredDetailViewModel
import com.project.tagger.repo.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GlobalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
        restartKoin()
        MobileAds.initialize(this, BuildConfig.ADMOB_KEY)
    }

    fun restartKoin() {
        stopKoin()
        startKoin {
            androidContext(this@GlobalApp)
            modules(
                listOf(
                    repositoryModule,
                    galleryModule,
                    registeredModule,
                    myModule
                )
            )
        }
    }
}

val repositoryModule = module {
    single<GalleryRepository> { LocalGalleryRepository(get(), get()) }
    single<RepoRepository> { RepoRepositoryImpl(get(), get()) }
    single { UserRepository(get(), get()) }
    single<PreferenceModel> { PreferenceModelImpl(get()) }
    single { AppDatabase.getInstance(get()) }

    factory { GetReposUC(get(), get()) }
    factory { PostRepoUC(get()) }
    factory { AddRepoUC(get(), get()) }
    factory { FindRepoUC(get()) }

    factory { RepoSearchViewModel(get(), get(), get()) }
}

val galleryModule = module {
    factory { GetPhotosFromGalleryUC(get()) }
    factory { GalleryViewModel(get(), get(), get()) }
}

val registeredModule = module {
    factory { GetRegisteredPhotosUC(get()) }
    factory { RegisterTagsOnPhotosUC(get(), get()) }
    factory { GetPopularTagsUC(get()) }
    factory { DeletePhotoUC(get()) }
    factory { RegisteredDetailViewModel(get(), get(), get()) }
    factory { RegisteredGalleryViewModel(get(), get(), get(), get()) }
    factory { TagViewModel(get(), get()) }

}

val myModule = module {
    factory { UpdateUserUC(get()) }
    factory { GetUserUC(get()) }
    factory { SignOutUC(get(), get(), get()) }


    factory { MyViewModel(get(), get(), get(), get(), get()) }

}