package com.project.tagger.registeredGallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.GetRegisteredPhotosUC
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy

class RegisteredGalleryViewModel(
    val getRegisteredPhotosUC: GetRegisteredPhotosUC,
    val getReposUC: GetReposUC
) {
    val photos = MutableLiveData<List<PhotoEntity>>()
    val currentRepo = MutableLiveData<RepoEntity>()
    val isBackUp = MutableLiveData<Boolean>().apply { value = false }


    private fun getRepo(): Single<RepoEntity> {
        return getReposUC.execute()
            .map { it.first() }
            .doOnSuccess {
                this.currentRepo.value = it
                this.isBackUp.value = it.isBackUp
            }

    }

    fun init() {
        getRepo()
            .map { it.photos }
//        getRegisteredPhotosUC.execute()
            .subscribeBy(
                onSuccess = {
                    photos.value = it
                },
                onError = { Log.i(tag(), it.message) })
    }

    fun query(query: String) {
        getRepo().map {
            it.photos.filter { it.tags.map { it.tag }.any { it.contains(query) } }
        }
//        getRegisteredPhotosUC.execute(query)
            .subscribeBy(
                onSuccess = {
                    photos.value = it
                },
                onError = { Log.i(tag(), it.message) })
    }

}