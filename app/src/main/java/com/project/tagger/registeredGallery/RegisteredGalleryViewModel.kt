package com.project.tagger.registeredGallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.GetPopularTagsUC
import com.project.tagger.gallery.GetRegisteredPhotosUC
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.TagEntity
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy

class RegisteredGalleryViewModel(
    val getRegisteredPhotosUC: GetRegisteredPhotosUC,
    val getReposUC: GetReposUC,
    val getPopularTagsUC: GetPopularTagsUC
) {
    val photos = MutableLiveData<List<PhotoEntity>>()
    val currentRepo = MutableLiveData<RepoEntity>()
    val isBackUp = MutableLiveData<Boolean>().apply { value = false }
    val popularTags = MutableLiveData<List<TagEntity>>()
    val isInSearchMode = MutableLiveData<Boolean>().apply { value = false }

    private fun getRepo(): Single<RepoEntity> {
        return getReposUC.execute()
            .map { it.first() }
            .doOnSuccess {
                this.currentRepo.value = it
                this.isBackUp.value = it.isBackUp
            }

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
}