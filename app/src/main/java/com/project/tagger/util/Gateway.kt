package com.project.tagger.util

import android.util.Log
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.atomic.AtomicReference

class Gateway<T : Any> private constructor(private var observable: Single<T>) : Single<T>() {

    private var disposable: Disposable? = null
    private var value: T? = null
    private val observers = mutableListOf<SingleObserver<in T>>()
    private val currentState: AtomicReference<STATE> = AtomicReference(STATE.READY)

    fun getCurrentState(): STATE? {
        return currentState.get()
    }

    override fun subscribeActual(observer: SingleObserver<in T>) {
        when (currentState.get()) {
            STATE.READY -> {
                disposable?.dispose()
                observers.add(observer)
                disposable = observable
                        .doOnDispose { Log.i(tag(), "Subscription disposed.") }
                        .doOnSubscribe {
                            Log.i(tag(), "Subscription start. CurrentState : $currentState")
                            currentState.set(STATE.PENDING)
                        }
                        .subscribeBy(
                                onSuccess = {
                                    currentState.set(STATE.DONE)
                                    value = it
                                    Log.i(tag(), "Subscription done.")
                                    observers.forEach { obs ->
                                        Log.i(tag(), "Observer : ${obs.hashCode()}")
                                        obs.onSuccess(it)
                                    }
                                    observers.clear()
                                },
                                onError = {
                                    currentState.set(STATE.READY)
                                    value = null
                                    Log.i(tag(), "Subscription error. ${it.message}")
                                    observers.forEach { obs ->
                                        obs.onError(it)
                                    }
                                    observers.clear()
                                }
                        )
            }
            STATE.DONE -> {
                observer.onSuccess(value!!)
                Log.i(tag(), "Subscription done. CurrentState : $currentState")
            }
            STATE.PENDING -> {
                Log.i(tag(), "Subscription exist. CurrentState : $currentState ${observer.hashCode()}")
                observers.add(observer)
            }
        }
    }

    /**
     *
     */
    fun setCache(value: T) {
        if (currentState.get() == STATE.DONE) {
            this.value = value
        }
    }

    fun clearRequest() {
        if (currentState.get()!= STATE.PENDING){
            currentState.set(STATE.READY)
        }
        value = null
        Log.i(tag(), "Subscription cleared. CurrentState : $currentState")
    }


    companion object {
        fun <T : Any> from(observable: Single<T>): Gateway<T> {
            return Gateway(observable)
        }
    }

    enum class STATE {
        READY,
        PENDING,
        DONE
    }
}
