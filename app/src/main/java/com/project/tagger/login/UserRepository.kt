package com.project.tagger.login

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.project.tagger.database.PreferenceModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.project.tagger.R
import com.project.tagger.database.FirebaseConstants.Companion.USER
import io.reactivex.Completable
import io.reactivex.Single
import org.koin.core.context.stopKoin

class UserRepository(
    val context: Context,
    val preferenceModel: PreferenceModel
) {
    companion object {

        const val USER_PREF = "USER_PREF"
    }

    val db = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        firestoreSettings = settings

    }

    val auth = FirebaseAuth.getInstance()

    fun updateUser(user: UserEntity): Completable {

        return Single.create<UserEntity> { emitter ->

            db.runTransaction { transaction ->
                val oldUser = transaction.get(db.collection(USER).document(user.email))
                    .toObject(UserEntity::class.java)
                val newUser: UserEntity
                if (oldUser != null) {
                    newUser = user.copy(repoReferences = oldUser.repoReferences)
                    transaction.set(
                        db.collection(USER).document(user.email),
                        newUser,
                        SetOptions.merge()
                    )
                } else {
                    newUser = user
                    transaction.set(
                        db.collection(USER).document(user.email),
                        user
                    )
                }
                newUser
            }.addOnFailureListener { emitter.onError(it) }
                .addOnSuccessListener { emitter.onSuccess(it) }
        }.doOnSuccess {
            preferenceModel.putPref(USER_PREF, Gson().toJson(it))

        }.toCompletable()
//        return db.collection(USER).document(user.email)
//            .toCompletable()
//            .doOnComplete {
//                preferenceModel.putPref(USER_PREF, Gson().toJson(user))
//            }
    }

    fun getUser(): UserEntity? {
        val userString = preferenceModel.getPref(USER_PREF, "")
        return if (userString.isEmpty()) {
            null
        } else {
            val user = Gson().fromJson<UserEntity>(userString, UserEntity::class.java)
            user
        }
    }

    fun signOut(): Completable {
        return Completable.fromAction {
            var googleSignInClient: GoogleSignInClient
            val gso: GoogleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut()
            auth.signOut()
            preferenceModel.putPref(USER_PREF, "")
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