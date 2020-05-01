package com.project.tagger.gallery

import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
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
    val photos = MutableLiveData<List<PhotoViewItem>>()
    val hasSelectedPhotos = MutableLiveData<Boolean>().apply { value = false }
    val selectedPhotos = mutableSetOf<PhotoEntity>()
    val defaultPath: String = Environment.getExternalStorageDirectory().path
    val folderStack = Stack<String>()


    var currentRepo: RepoEntity? = null

    fun init(path: String = defaultPath, back: Boolean = false) {

        if (currentRepo == null) {
            getReposUC.execute()
                .map { it.first() }
                .doOnSuccess { this.currentRepo = it }
        } else {
            Single.just(currentRepo)
        }
            .flatMap {
                getPhotosFromGalleryUC.execute( path)
            }
            .subscribeBy(
                onSuccess = {
                    if (!back) {
                        folderStack.push(path)
                    }
                    photos.value = it.map { PhotoViewItem(it, false) }
                    Log.i(tag(), it.size.toString())
                    it.subList(0, min(10, it.size)).forEach {
                        Log.i(tag(), "${it.path} ${it.folderName}")
                    }
                },
                onError = { Log.i(tag(), it.message) })
    }

    fun hasBackStack(): Boolean {
        Log.i(tag(), "hasBackStack ${folderStack.peekIfNotEmpty()} ${folderStack.size}")
        return folderStack.isNotEmpty()
    }

    fun goBack() {
        Log.i(tag(), "goBack")
        folderStack.pop()
        folderStack.peekIfNotEmpty()?.let { init(it, true) }

    }

    fun register(photoEntity: PhotoEntity) {
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
                if (selectedPhotos.contains(it.photoEntity)) {
                    Log.i(tag(), "selected ${it.photoEntity.path}")
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

    data class PhotoViewItem(
        val photoEntity: PhotoEntity,
        val isSelected: Boolean = false
    )
}