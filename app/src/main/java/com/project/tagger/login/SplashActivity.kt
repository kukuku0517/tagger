package com.project.tagger.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.project.tagger.R
import com.project.tagger.main.MainActivity
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {
    val getUserUC: GetUserUC by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        getUserUC.execute()
            .delay(800, TimeUnit.MILLISECONDS)
            .subscribeBy(onSuccess = {
                startMainActivity()
            }, onComplete = {
                startLoginActivity()
            }, onError = {
                startLoginActivity()
            })

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
