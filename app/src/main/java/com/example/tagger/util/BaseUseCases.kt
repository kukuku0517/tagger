package com.example.tagger.util

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single


interface UseCaseCompletable<in Params> {
    fun execute(params: Params? = null): Completable
}

interface UseCaseSingle<in Params, Results> {
    fun execute(params: Params?=null ): Single<Results>
}

interface UseCaseObservable<in Params, Results> {
    fun execute(params: Params? = null): Observable<Results>
}

interface UseCaseMaybe<in Params, Results> {
    operator fun invoke(params: Params? = null): Maybe<Results>
}

class UseCaseParameterNullPointerException : Exception(){
    override val message: String?
        get() = "Required non-null params is null."
}
