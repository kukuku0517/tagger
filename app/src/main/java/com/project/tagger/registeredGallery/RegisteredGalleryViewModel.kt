package com.project.tagger.registeredGallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.GetPopularTagsUC
import com.project.tagger.gallery.GetRegisteredPhotosUC
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.TagEntity
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.UserEntity
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy

class RegisteredGalleryViewModel(
    val getRegisteredPhotosUC: GetRegisteredPhotosUC,
    val getReposUC: GetReposUC,
    val getPopularTagsUC: GetPopularTagsUC,
    val getUserUC: GetUserUC
) {
    enum class RepoUserState {
        PRO,
        BASIC,
        VISITOR
    }

    val photos = MutableLiveData<List<PhotoEntity>>()
    val currentRepo = MutableLiveData<RepoEntity>()
    val repos = MutableLiveData<List<RepoEntity>>()
    val repoAuthState = MutableLiveData<RepoUserState>().apply { value = RepoUserState.BASIC }
    val popularTags = MutableLiveData<List<TagEntity>>()
    val isInSearchMode = MutableLiveData<Boolean>().apply { value = false }


    private fun getRepo(): Single<RepoEntity> {
        return getReposUC.execute()
            .doOnSuccess { repos.value = it }
            .map { repos ->
                val currentRepoName = currentRepo.value?.name
                if (currentRepoName == null) {
                    repos.first()
                } else {
                    repos.firstOrNull { it.name == currentRepoName } ?: repos.first()
                }
            }
            .zipWith(
                getUserUC.execute().toSingle(),
                BiFunction<RepoEntity, UserEntity, RepoEntity> { repo, user ->
                    this.currentRepo.value = repo
                    when {
                        repo.owner != user.email -> {
                            repoAuthState.value = RepoUserState.VISITOR
                        }
                        repo.backUp -> {
                            repoAuthState.value = RepoUserState.PRO
                        }
                        else -> {
                            repoAuthState.value = RepoUserState.BASIC

                        }
                    }

                    repo
                })

    }

    fun init() {
        getPopularTagsUC.execute()
            .subscribeBy(
                onSuccess = {
                    popularTags.value = it
                },
                onError = {

                }
            )

        getRepo()
            .map { it.photos.sortedByDescending { it.updatedAt } }
//        getRegisteredPhotosUC.execute()
            .subscribeBy(
                onSuccess = {
                    photos.value = it
                },
                onError = { Log.i(tag(), it.message) })
    }

    fun toggleSearch() {
        isInSearchMode.value = !isInSearchMode.value!!
    }

    fun query(query: List<String>) {
        getRepo().map {
            it.photos.filter {
                it.tags.map { it.tag }.reduce { acc, s -> "$acc#$s" }.contains(query)
            }
        }.subscribeBy(
            onSuccess = {
                photos.value = it
            },
            onError = { Log.i(tag(), it.message) })
    }

    fun String.contains(queries: List<String>): Boolean {
        queries.forEach {
            if (this.contains(it)) {
                return true
            }
        }
        return false
    }

    fun openRepo(name: String) {
        repos.value?.let { repos ->
            repos.firstOrNull { it.name == name }?.let { repo ->
                currentRepo.value = repo
                init()
            }
        }

    }
}