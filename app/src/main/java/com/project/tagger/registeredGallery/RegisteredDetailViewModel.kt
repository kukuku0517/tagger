package com.project.tagger.registeredGallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.*
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.MutableLiveEvent
import com.project.tagger.util.Time
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import java.util.ArrayList

data class TagViewItem(
    val tag: TagEntity,
    val isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (other !is TagViewItem) return false
        return tag.tag == other.tag.tag
    }

    override fun hashCode(): Int {
        return tag.tag.hashCode()
    }
}

class RegisteredDetailViewModel(
    val registerTagsOnPhotosUC: RegisterTagsOnPhotosUC,
    val getPopularTagsUC: GetPopularTagsUC,
    val deletePhotoUC: DeletePhotoUC
) {
    var photos: PhotoEntity? = null
    val tags = MutableLiveData<MutableSet<com.project.tagger.tag.TagViewItem>>().apply {
        value = mutableSetOf()
    }
    val finishEvent = MutableLiveEvent(false)
    var repo: RepoEntity? = null

    fun setTags(tags: ArrayList<TagEntity>?) {
        if (tags.isNullOrEmpty()) {
            getPopularTagsUC.execute()
                .map { it.map { com.project.tagger.tag.TagViewItem(it, isSelected = false) } }
        } else {
            Single.just(tags)
                .map { it.map { com.project.tagger.tag.TagViewItem(it, isSelected = true) } }
        }
            .doOnSubscribe { isLoading.value = true }

            .subscribeBy(
                onSuccess = {
                    isLoading.value = false
                    this.tags.value = this.tags.value!!.apply { addAll(it) }.sortedBy { it.tag.tag }
                        .toMutableSet()
                },
                onError = {
                    isLoading.value = false
                    Log.i(tag(), it.message)
                })
    }


    val isLoading = MutableLiveData<Boolean>().apply { value = false }

    fun addTag(text: String) {
        tags.value = tags.value!!.apply {
            val item = com.project.tagger.tag.TagViewItem(TagEntity(text, -1), true)
            remove(item)
            add(item)
        }.sortedBy { it.tag.tag }.toMutableSet()
    }

    fun removeTag(text: String) {
        tags.value = tags.value!!.apply {
            val item = com.project.tagger.tag.TagViewItem(TagEntity(text, -1), false)
            remove(item)
            add(item)
        }.sortedBy { it.tag.tag }.toMutableSet()
    }

    fun updateTags() {

        photos?.let { photos->
            registerTagsOnPhotosUC.execute(
                RegisterTagsOnPhotosUC.RegisterTagParam(
                    tags = tags.value!!.filter { it.isSelected }.map { it.tag },
                    photos = listOf(photos),
                    repo = repo!!,
                    type = RegisterTagsOnPhotosUC.RegisterType.REPLACE,
                    updatedAt = Time().toTimestamp()
                )
            )
                .doOnSubscribe { isLoading.value = true }
                .subscribeBy(
                    onSuccess = {
                        isLoading.value = false
                        Log.i(tag(), "tagged ${it.size} photos")
                        finishEvent.value = true
                    },
                    onError = {
                        isLoading.value = false
                        Log.i(tag(), it.message)
                    })
        }
    }

    fun delete(){
        photos?.let { photos->
            deletePhotoUC.execute(photos)
                .doOnSubscribe { isLoading.value = true }
                .subscribeBy(
                    onSuccess = {
                        isLoading.value = false
                        finishEvent.value = true
                    },
                    onError = {
                        isLoading.value = false
                        Log.i(tag(), it.message)
                    })
        }
    }
}