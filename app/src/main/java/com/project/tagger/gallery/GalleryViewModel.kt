package com.project.tagger.gallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.project.tagger.login.GetUserUC
import com.project.tagger.repo.GetReposUC
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.peekIfNotEmpty
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import java.util.*
import kotlin.math.min

class GalleryViewModel(
    val getPhotosFromGalleryUC: GetPhotosFromGalleryUC,
    val getReposUC: GetReposUC,
    val getUserUC: GetUserUC
) {
    private val photos = MutableLiveData<List<PhotoViewItem>>()


    val selectedPhotoSize = MutableLiveData<Int>().apply { value = 0 }
    val selectedPhotos = mutableSetOf<GalleryEntity>()
    val defaultPath: String = ""
    val folderStack = Stack<String>()
    val currentPath = MutableLiveData<String>().apply { value = defaultPath }

    val isLoading = MutableLiveData<Boolean>().apply { value = false }

    val filterEnabled = MutableLiveData<Boolean>().apply { value = false }

    val photosFiltered = Transformations.map(photos) {
        if (filterEnabled.value!!) {
            it.filter { !it.galleryEntity.isRegistered }
        } else {
            it
        }
    }

    fun showLoading(show: Boolean) {
        isLoading.postValue(show)
    }

    val gridType = Transformations.map(currentPath) {
        if (it.isEmpty()) {
            1
        } else {
            0
        }
    }

    var currentRepo: RepoEntity? = null

    fun init(path: String = defaultPath, back: Boolean = false) {

        if (currentRepo == null) {
            getReposUC.execute()
                .flatMap { repos ->
                    getUserUC.execute()
                        .toSingle()
                        .map { user -> repos.firstOrNull { it.owner == user.email } }
                }
                .doOnSuccess { this.currentRepo = it }
        } else {
            Single.just(currentRepo)
        }
            .doOnSubscribe { showLoading(true) }
            .flatMap {
                getPhotosFromGalleryUC.execute(path)
            }

            .doOnSuccess { currentPath.value = path }
            .subscribeBy(
                onSuccess = {
                    showLoading(false)
                    if (!back) {
                        folderStack.push(path)
                    }
                    photos.value = it.map { PhotoViewItem(it, false) }
                    Log.i(tag(), it.size.toString())

                },
                onError = {
                    showLoading(false)
                    Log.i(tag(), it.message)
                })
    }

    fun hasBackStack(): Boolean {
        Log.i(tag(), "hasBackStack ${folderStack.peekIfNotEmpty()} ${folderStack.size}")
        return folderStack.isNotEmpty()
    }

    fun unselectAll() {
        selectedPhotos.clear()
        updateSelected()
    }

    fun goBack() {
        Log.i(tag(), "goBack")
        folderStack.pop()
        folderStack.peekIfNotEmpty()?.let { init(it, true) }
    }

    fun register(photoEntity: GalleryEntity) {
        if (selectedPhotos.contains(photoEntity)) {
            selectedPhotos.remove(photoEntity)
        } else {
            selectedPhotos.add(photoEntity)
        }
        updateSelected()
    }

    private fun updateSelected() {
        Log.i(tag(), "${selectedPhotos.size} selectedPhotos")

        selectedPhotoSize.value = selectedPhotos.size
        photos.value?.let {
            val newPhotos = it.map {
                if (selectedPhotos.contains(it.galleryEntity)) {
                    Log.i(tag(), "selected ${it.galleryEntity.path}")
                    it.copy(
                        isSelected = true
                    )
                } else {
                    it.copy(
                        isSelected = false
                    )
                }
            }
            photos.value = newPhotos
        }
    }

    fun toggleRegisterFilter() {
        filterEnabled.value = !filterEnabled.value!!
        photos.value = photos.value
    }

    data class PhotoViewItem(
        val galleryEntity: GalleryEntity,
        val isSelected: Boolean = false
    )
}