package com.project.tagger.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.project.tagger.R
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable

class Initializer(
    val context: Context
) {
    companion object {

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }


    fun isInitialized(): Boolean {
        return hasAllPermissions(*permissions)

    }

    class InitializeException(val errorCode: Int) : Exception() {
        companion object {

            const val ERROR_PERMISSION = 10
            const val ERROR_SESSION = 11
            const val ERROR_MIGRATION = 12
            const val ERROR_TRIAL_REFRESH = 13
        }
    }


    private fun hasAllPermissions(vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(
                    context,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }


    fun getPermissions(
        activity: AppCompatActivity,
        vararg permissions: String
    ): Observable<Boolean> {

        return if (permissions.isEmpty() || hasAllPermissions(*permissions)) {
            Observable.just(true)
        } else {

            var permissionMessage = context.getString(R.string.request_permission_message)
            Observable
                .create<Boolean> { emitter ->
                    AlertDialog.Builder(activity)
                        .setTitle(context.getString(R.string.request_permission))
                        .setMessage(permissionMessage)
                        .setPositiveButton(context.getString(R.string.okay)) { dialog, _ ->
                            emitter.onNext(true)
                            dialog.dismiss()
                        }
                        .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                            emitter.onError(InitializeException(InitializeException.ERROR_PERMISSION))
                            dialog.dismiss()
                        }
                        .create().show()
                }
                .flatMap { RxPermissions(activity).request(*permissions) }
                .map { granted ->
                    if (granted) granted
                    else throw InitializeException(InitializeException.ERROR_PERMISSION)
                }
        }
    }


}