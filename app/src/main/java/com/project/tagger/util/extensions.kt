package com.project.tagger.util

import android.view.View
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import io.reactivex.Completable
import io.reactivex.Maybe
import java.util.*

fun Any.tag(): String {
    return this.javaClass.simpleName
}

fun View.show(show: Boolean = true) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}

fun <T> Stack<T>.peekIfNotEmpty(): T? {
    return if (isNotEmpty()){
        peek()
    }else{
        null
    }
}



inline fun <reified T : Any> DocumentReference.asMaybe(): Maybe<T> {

    return Maybe.create<T> { emitter ->

        val listenerRegistration = this
            .addSnapshotListener { snapshot, e ->

                if (e != null) {

                    emitter.onError(e)

                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val documentModel = snapshot.toObject(T::class.java)
                    if (documentModel == null) {
                        emitter.onComplete()
                    } else {
                        emitter.onSuccess(documentModel)
                    }
                } else {
                    emitter.onComplete()
                }
            }

        emitter.setCancellable {
            listenerRegistration.remove()
        }
    }
}

inline fun <T> Task<T>.asCompletable(): Completable {
    return Completable.create { emitter ->
        this
            .addOnSuccessListener {
                emitter.onComplete()
            }
            .addOnFailureListener { e ->
                emitter.onError(e)
            }
    }
}


inline fun <reified T : Any> CollectionReference.asMaybe(): Maybe<List<T>> {

    return Maybe.create<List<T>> { emitter ->

        val listenerRegistration = this
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    emitter.onError(e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val documentModel = snapshot.documents.mapNotNull { it.toObject(T::class.java) }
                    emitter.onSuccess(documentModel)
                } else {
                    emitter.onComplete()
                }
            }

        emitter.setCancellable {
            listenerRegistration.remove()
        }
    }
}