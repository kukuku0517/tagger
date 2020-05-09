package com.project.tagger.my

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.SignOutUC
import com.project.tagger.login.UserEntity
import com.project.tagger.repo.AddRepoUC
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.PostRepoUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.MutableLiveEvent
import com.project.tagger.util.tag
import io.reactivex.rxkotlin.subscribeBy

class MyViewModel(
    val userUC: GetUserUC,
    val getReposUC: GetReposUC,
    val signOutUC: SignOutUC,
    val postRepoUC: PostRepoUC,
    val addRepoUC: AddRepoUC
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
                    Log.i(tag(), "Signout comp")
                    signOutEvent.value = true
                },
                onError = {
                    Log.i(tag(), "Signout err ${it.message}")
                }
            )
    }

    fun setPremium(repoEntity: RepoEntity) {
//        if (repoEntity.isBackUp) {
//
//        } else {
        postRepoUC.execute(repoEntity.copy(backUp = true))
            .flatMap { getReposUC.execute() }
            .subscribeBy(
                onSuccess = {
                    repos.value = it
                },
                onError = {
                    Log.i(tag(), "Signout err ${it.message}")
                }
            )
    }

    fun addRepo(id: Int) {
        addRepoUC.execute(id)
            .subscribeBy(
                onSuccess = {
                  init()
                },
                onError = {
                    Log.i(tag(), "addRepo err ${it.message}")
                }
            )
    }
//    }

}
