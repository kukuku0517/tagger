package com.project.tagger.login

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.gson.Gson
import com.project.tagger.BuildConfig
import com.project.tagger.R
import com.project.tagger.database.PreferenceModel
import com.project.tagger.main.MainActivity
import com.project.tagger.util.AlertObservable
import com.project.tagger.util.RemoteConfigManager
import com.project.tagger.util.VersionUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {
    val getUserUC: GetUserUC by inject()
    val preferenceModel: PreferenceModel by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        RemoteConfigManager().init()
            .andThen(checkVersion())
            .andThen(
                getUserUC.execute()
            )
            .subscribeBy(onSuccess = {
                startMainActivity()
            }, onComplete = {
                startLoginActivity()
            }, onError = {
                startLoginActivity()
            })
    }


    data class Notice(
        val version: Int,
        val message: String
    )

    data class VersionInfo(
        val minVersion: String,
        val forced: Boolean,
        val message: String
    )

    private fun checkNotice() {
        val json = preferenceModel.getPref(RemoteConfigManager.NOTICE, "")
        val notice = Gson().fromJson(json, Notice::class.java)

    }

    private fun checkVersion(): Completable {
        return Completable.defer {

            val json = preferenceModel.getPref(RemoteConfigManager.VERSION_INFO, "")
            val versionInfo = Gson().fromJson(json, VersionInfo::class.java)

            val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
            val latestVersion = versionInfo.minVersion
            if (VersionUtil.compare(currentVersion, latestVersion) < 0) {
                Completable.complete()
            } else {
                AlertObservable.create(this, object : AlertObservable.AlertObservableListener() {
                    override fun positiveTitle(): String {
                        return getString(R.string.update)
                    }

                    override fun onPositive(dialog: DialogInterface) {
                        super.onPositive(dialog)
                    }

                    override fun negativeTitle(): String {
                        return if (versionInfo.forced) {
                            getString(R.string.exit)
                        } else {
                            getString(R.string.do_it_later)
                        }
                    }

                    override fun onNegative(dialog: DialogInterface) {
                        if (versionInfo.forced) {
                            finish()
                        }
                        super.onNegative(dialog)
                    }

                    override fun title(): String {
                        return getString(R.string.update_notice)
                    }

                    override fun message(): String {
                        return getString(R.string.update_message) + versionInfo.message
                    }

                })
            }
        }
    }

    private fun startLoginActivity() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
//        overridePendingTransition(R.anim.fade_out,R.anim.fade_in)
    }

    private fun startMainActivity() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
//        overridePendingTransition(R.anim.fade_out,R.anim.fade_in)
    }
}
