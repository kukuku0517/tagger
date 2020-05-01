package com.project.tagger.login

import android.content.Context
import com.project.tagger.database.PreferenceModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.gson.Gson
import com.project.tagger.database.FirebaseConstants.Companion.USER
import io.reactivex.Completable

class UserRepository(
    val context: Context,
    val preferenceModel: PreferenceModel
) {
    companion object {

        const val USER_PREF = "USER_PREF"
    }

    val db = FirebaseFirestore.getInstance()

    fun updateUser(user: UserEntity): Completable {
        return db.collection(USER).document(user.email).set(user)
            .toCompletable()
            .doOnComplete {
                preferenceModel.putPref(USER_PREF, Gson().toJson(user))
            }
    }

    fun getUser(): UserEntity? {
        val userString = preferenceModel.getPref(USER_PREF, "")
        return if (userString.isEmpty()){
            null
        }else{
            val user = Gson().fromJson<UserEntity>(userString, UserEntity::class.java)
            user
        }
    }
}

fun Task<Void>.toCompletable(): Completable {
    return Completable.create { emitter ->
        this.addOnSuccessListener { emitter.onComplete() }
            .addOnFailureListener { emitter.onError(it) }
            .addOnCompleteListener { emitter.onComplete() }
    }
}