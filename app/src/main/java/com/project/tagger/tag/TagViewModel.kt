package com.project.tagger.tag

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.RegisterTagsOnPhotosUC
import com.project.tagger.gallery.TagEntity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.MutableLiveEvent
import com.project.tagger.util.tag
import io.reactivex.rxkotlin.subscribeBy
import java.util.ArrayList

class TagViewModel(
    val registerTagsOnPhotosUC: RegisterTagsOnPhotosUC
) {
    var photos: List<PhotoEntity> = listOf()
    val tags = MutableLiveData<List<TagEntity>>().apply { value = listOf() }
    val finishEvent = MutableLiveEvent<Boolean>(false)
    var repo: RepoEntity? = null
    fun setPhotos(photos: ArrayList<PhotoEntity>?) {
        if (photos != null) {
            this.photos = photos
        }
    }

    fun addTag(text: String) {
        tags.value = tags.value?.toMutableList()?.apply { add(TagEntity(text)) }
    }

    fun updateTags() {

        registerTagsOnPhotosUC.execute(
            RegisterTagsOnPhotosUC.RegisterTagParam(
                tags.value!!,
                photos,
                repo!!
            )
        ).subscribeBy(
            onSuccess = {
                Log.i(tag(), "tagged ${it.size} photos")
                finishEvent.value = true
            }, onError = {
                Log.i(tag(), it.message)
            })
    }
}