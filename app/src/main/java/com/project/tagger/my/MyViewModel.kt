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
    val signOutDialogEvent = MutableLiveEvent(false)
    val toastEvent = MutableLiveEvent<String>()
    val isLoading = MutableLiveData<Boolean>().apply { value = false }

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
        signOutDialogEvent.value = true

    }

    fun signOutConfirmed() {

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

    fun isMyRepo(repoEntity: RepoEntity): Boolean {
        return repoEntity.owner == user.value!!.email
    }

    fun syncRepository(repoEntity: RepoEntity) {
        if (isLoading.value == true) return

        if (isMyRepo(repoEntity)) {
            postRepoUC.execute(repoEntity)
                .doOnSubscribe { isLoading.value = true }
                .flatMap { getReposUC.execute() }
                .subscribeBy(
                    onSuccess = {
                        isLoading.value = false
                        repos.value = it
                        toastEvent.value = "Upload Success"
                    },
                    onError = {

                        isLoading.value = false
                        Log.i(tag(), "Signout err ${it.message}")
                    }
                )
        } else {
            getReposUC.execute(true)
                .doOnSubscribe { isLoading.value = true }
                .subscribeBy(
                    onSuccess = {
                        isLoading.value = false
                        repos.value = it
                        toastEvent.value = "Download Success"
                    },
                    onError = {
                        isLoading.value = false
                        Log.i(tag(), "Signout err ${it.message}")
                    }
                )
        }
    }

}
