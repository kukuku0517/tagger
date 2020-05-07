package com.project.tagger.gallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
    val getReposUC: GetReposUC
) {
    private val photos = MutableLiveData<List<PhotoViewItem>>()

    val photosFiltered = Transformations.map(photos) {
        if (filterEnabled) {
            it.filter { !it.galleryEntity.isRegistered }
        } else {
            it
        }
    }
    val hasSelectedPhotos = MutableLiveData<Boolean>().apply { value = false }
    val selectedPhotos = mutableSetOf<GalleryEntity>()
    val defaultPath: String = ""
    val folderStack = Stack<String>()
    val currentPath = MutableLiveData<String>().apply { value = defaultPath }

    val isLoading = MutableLiveData<Boolean>().apply { value = false }

    var  filterEnabled = false

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
                .map { it.first() }
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
                    it.subList(0, min(10, it.size)).forEach {
                        Log.i(tag(), "${it.path} ${it.folderName}")
                    }
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

    fun selectAll() {
        selectedPhotos.addAll(photos.value!!.map { it.galleryEntity }.filter { !it.isDirectory })
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

        hasSelectedPhotos.value = selectedPhotos.isNotEmpty()
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

    fun enableRegisterFilter(checked: Boolean) {
        filterEnabled = checked
        photos.value = photos.value
    }

    data class PhotoViewItem(
        val galleryEntity: GalleryEntity,
        val isSelected: Boolean = false
    )
}