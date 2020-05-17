package com.project.tagger.my

import androidx.lifecycle.MutableLiveData
import com.project.tagger.repo.*
import com.project.tagger.util.MutableLiveEvent
import io.reactivex.rxkotlin.subscribeBy

class RepoSearchViewModel(
    val addRepoUC: AddRepoUC,
    val findRepoUC: FindRepoUC,
    val getReposUC: GetReposUC
) {
    enum class RepoSearchState {
        OK,
        DUPLICATE,
        EMPTY,
        ERROR
    }

    val repoResult = MutableLiveData<RepoEntity?>().apply { value = null }
    val currentRepos = MutableLiveData<List<RepoEntity>>()
    val repoSearchResultState = MutableLiveData<RepoSearchState>()

    val repoAddCompleteEvent = MutableLiveEvent(false)

    val loading = MutableLiveData<Boolean>().apply { value = false }

    fun init() {
        getReposUC.execute()
            .subscribeBy(
                onSuccess = {
                    this.currentRepos.value = it
                },
                onError = {

                })
    }

    fun searchRepo(id: Int) {
        findRepoUC.execute(id)
            .subscribeBy(
                onSuccess = {
                    repoResult.value = it

                    if (currentRepos.value!!.map { it.id }.contains(it.id)) {
                        repoSearchResultState.value = RepoSearchState.DUPLICATE
                    } else {
                        repoSearchResultState.value = RepoSearchState.OK
                    }
                },
                onError = {
                    repoResult.value = null
                    repoSearchResultState.value = RepoSearchState.ERROR
                },
                onComplete = {
                    repoResult.value = null
                    repoSearchResultState.value = RepoSearchState.EMPTY
                })
    }

    fun addRepo() {
        if (repoSearchResultState.value == RepoSearchState.OK){
            addRepoUC.execute(repoResult.value)
                .subscribeBy(
                    onSuccess = {
                        repoAddCompleteEvent.value= true
                    },
                    onError = {

                    })
        }
    }

}