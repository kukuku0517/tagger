package com.project.tagger.tag

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.GetPopularTagsUC
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.RegisterTagsOnPhotosUC
import com.project.tagger.gallery.TagEntity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.MutableLiveEvent
import com.project.tagger.util.Time
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

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

class TagViewModel(
    val registerTagsOnPhotosUC: RegisterTagsOnPhotosUC,
    val getPopularTagsUC: GetPopularTagsUC
) {
    var photos: List<PhotoEntity> = listOf()
    val tags = MutableLiveData<MutableSet<TagViewItem>>().apply { value = mutableSetOf() }
    val recommendedTags =
        MutableLiveData<MutableSet<TagViewItem>>().apply { value = mutableSetOf() }

    val finishEvent = MutableLiveEvent(false)
    var repo: RepoEntity? = null

    fun setTags(tags: ArrayList<TagEntity>?) {

        if (tags != null) {
            Single.just(tags)
                .map { it.map { TagViewItem(it, isSelected = true) } }
                .doOnSubscribe { isLoading.value = true }
                .subscribeBy(
                    onSuccess = {
                        isLoading.value = false
                        this.tags.value =
                            this.tags.value!!.apply { addAll(it) }.sortedBy { it.tag.tag }
                                .toMutableSet()
                    },
                    onError = {
                        isLoading.value = false
                        Log.i(tag(), it.message)
                    })
        }

        getPopularTagsUC.execute()
            .map { it.map { TagViewItem(it, isSelected = false) }.subList(0, min(10, it.size)) }
            .subscribeBy(
                onSuccess = {
                    isLoading.value = false
                    this.recommendedTags.value =
                        this.recommendedTags.value!!.apply { addAll(it) }.sortedBy { it.tag.tag }
                            .toMutableSet()
                },
                onError = {
                    isLoading.value = false
                    Log.i(tag(), it.message)
                })
    }

    fun setPhotos(photos: ArrayList<PhotoEntity>?) {
        if (photos != null) {
            this.photos = photos
        }
    }

    val isLoading = MutableLiveData<Boolean>().apply { value = false }

    fun addTag(text: String) {
        tags.value = tags.value!!.apply {
            val item = TagViewItem(TagEntity(text, -1), true)
            remove(item)
            add(item)
        }.sortedBy { it.tag.tag }.toMutableSet()

    }

    fun removeTag(text: String) {
        tags.value = tags.value!!.apply {
            val item = TagViewItem(TagEntity(text, -1), false)
            remove(item)
            add(item)
        }.sortedBy { it.tag.tag }.toMutableSet()
    }

    fun updateTags() {

        registerTagsOnPhotosUC.execute(
            RegisterTagsOnPhotosUC.RegisterTagParam(
                tags = tags.value!!.filter { it.isSelected }.map { it.tag },
                photos = photos,
                repo = repo!!,
                type = RegisterTagsOnPhotosUC.RegisterType.MERGE,
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