package com.project.tagger.util

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import io.reactivex.Completable
import io.reactivex.Observable

class AlertObservable {
    abstract class AlertObservableListener {
        open fun positiveTitle() = "확인"
        open fun onPositive(dialog: DialogInterface): Unit {
            dialog.dismiss()
        }

        open fun negativeTitle() = "취소"
        open fun onNegative(dialog: DialogInterface): Unit {
            dialog.dismiss()
        }

        open fun title() = ""

        open fun message() = ""


    }

    companion object {
        fun create(context: Context, listener: AlertObservableListener): Completable {
            return Completable.create { emitter ->
                AlertDialog.Builder(context)
                    .setPositiveButton(listener.positiveTitle()) { dialog, which ->
                        listener.onPositive(dialog)
                        emitter.onComplete()
                    }
                    .setNegativeButton(listener.negativeTitle()) { dialog, which ->
                        listener.onNegative(dialog)
                        emitter.onComplete()
                    }
                    .setOnCancelListener {
                        emitter.onComplete()
                    }
                    .setTitle(listener.title())
                    .setMessage(listener.message())
                    .show()
            }
        }
    }
}