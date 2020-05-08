package com.project.tagger.my

import androidx.lifecycle.MutableLiveData
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.SignOutUC
import com.project.tagger.login.UserEntity
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.MutableLiveEvent
import io.reactivex.rxkotlin.subscribeBy

class MyViewModel(
    val userUC: GetUserUC,
    val getReposUC: GetReposUC,
    val signOutUC: SignOutUC
) {

    val user = MutableLiveData<UserEntity>()
    val repos = MutableLiveData<List<RepoEntity>>()
    val signOutEvent = MutableLiveEvent(false)

    fun init() {
        userUC.execute()
            .doOnSuccess { user.value = it }
            .flatMapSingle { getReposUC.execute() }
            .subscribeBy(
                onSuccess = {
                    repos.value = it
                },
                onError = {})
    }

    fun signOut() {
        signOutUC.execute()
            .subscribeBy(
                onComplete = {
                    signOutEvent.value = true
                },
                onError = {

                }
            )
    }

}
