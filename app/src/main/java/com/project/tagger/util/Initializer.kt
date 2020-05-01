package com.project.tagger.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

            var permissionMessage = "특정 기능을 사용하기 위해 아래의 권한이 필요합니다.\n" +
                    "\n"
            if (permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                permissionMessage +=
                    "[저장소](선택)\n" +
                            "- 프로필 설정, 미션 인증(사진 전송)을 위한 갤러리 접근\n" +
                            "- 미션 인증 사진 자동저장\n" +
                            "\n"
            }
            if (permissions.contains(Manifest.permission.CAMERA)) {
                permissionMessage +=
                    "카메라[선택]\n" +
                            "- 프로필, 미션 인증 사진 직접 촬영\n" +
                            "\n"
            }

            permissionMessage += "[다시 묻지 않음]을 설정하신 경우에는\n앱정보>권한 승인 후 이용해주세요."

            Observable
                .create<Boolean> { emitter ->
                    AlertDialog.Builder(activity)
                        .setTitle("권한 안내")
                        .setMessage(permissionMessage)
                        .setPositiveButton("확인") { dialog, _ ->
                            emitter.onNext(true)
                            dialog.dismiss()
                        }
                        .setNegativeButton("취소") { dialog, _ ->
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