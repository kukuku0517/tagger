package com.example.tagger.tag

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.tagger.gallery.PhotoEntity
import com.example.tagger.gallery.RegisterTagsOnPhotosUC
import com.example.tagger.gallery.TagEntity
import com.example.tagger.util.MutableLiveEvent
import com.example.tagger.util.tag
import io.reactivex.rxkotlin.subscribeBy
import java.util.ArrayList

class TagViewModel(
    val registerTagsOnPhotosUC: RegisterTagsOnPhotosUC
) {
    var photos: List<PhotoEntity> = listOf()
    val tags =  MutableLiveData<List<TagEntity>>().apply { value = listOf() }
    val finishEvent = MutableLiveEvent<Boolean>(false)
    fun setPhotos(photos: ArrayList<PhotoEntity>?) {
        if (photos != null) {
            this.photos = photos
        }
    }

    fun addTag(text: String) {
        tags.value = tags.value?.toMutableList()?.apply { add(TagEntity(text))}
    }

    fun updateTags() {
        registerTagsOnPhotosUC.execute(
            tags.value!! to photos
        ).subscribeBy(
            onSuccess = {
                Log.i(tag(), "tagged ${it.size} photos")
                finishEvent.value = true
            }, onError = {
                Log.i(tag(), it.message)
            })
    }
}