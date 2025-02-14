package com.project.tagger.util

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

fun Any.tag(): String {
    return this.javaClass.simpleName
}

fun View.show(show: Boolean = true) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}

fun View.showInvisible(show: Boolean = true, animated: Long = 0) {
    this.visibility = if (show) View.VISIBLE else View.INVISIBLE
    if (animated > 0) {
        val fadeIn = if (show) {
            AlphaAnimation(0f, 1f)
        } else {
            AlphaAnimation(1f, 0f)
        }
        fadeIn.interpolator = DecelerateInterpolator() //add this
        fadeIn.duration = animated
        this.animation = fadeIn
    }
}

fun <T> Stack<T>.peekIfNotEmpty(): T? {
    return if (isNotEmpty()) {
        peek()
    } else {
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


fun <T> Single<T>.fromIO(): Single<T> {
    return this.subscribeOn(Schedulers.io())
}

fun <T> Single<T>.toUI(): Single<T> {
    return this.observeOn(AndroidSchedulers.mainThread())
}

fun Completable.fromIO(): Completable {
    return this.subscribeOn(Schedulers.io())
}

fun Completable.toUI(): Completable {
    return this.observeOn(AndroidSchedulers.mainThread())
}

public inline fun <S, T : S> Iterable<T>.reduceIfEmpty(default: S, operation: (acc: S, T) -> S): S {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return default
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }
    return accumulator
}

fun Int.modPositive(divider: Int): Int {
    val rem = this.rem(divider)
    return if (rem < 0) rem + divider else rem
}