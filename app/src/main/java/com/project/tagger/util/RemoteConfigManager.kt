package com.project.tagger.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.project.tagger.BuildConfig
import com.project.tagger.database.PreferenceModel
import io.reactivex.Completable
import org.koin.core.KoinComponent

class RemoteConfigManager : KoinComponent {
    companion object {
        const val NOTICE = "NOTICE"
        const val VERSION_INFO = "VERSION_INFO"
    }

    val preferenceModel: PreferenceModel by getKoin().inject()

    fun init(): Completable {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        return Completable.create { emitter ->
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        remoteConfig.activateFetched()
                        preferenceModel.putPref(NOTICE, remoteConfig.getString(NOTICE))
                        preferenceModel.putPref(VERSION_INFO, remoteConfig.getString(VERSION_INFO))

                        emitter.onComplete()
                    } else {
                        task.exception?.let {
                            emitter.onError(it)
                        } ?: kotlin.run {
                            emitter.onComplete()
                        }
                    }
                }
        }
    }
}