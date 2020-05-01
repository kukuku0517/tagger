package com.project.tagger.registeredGallery

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.tagger.gallery.GetRegisteredPhotosUC
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.util.tag
import io.reactivex.rxkotlin.subscribeBy

class RegisteredGalleryViewModel(
  val getRegisteredPhotosUC: GetRegisteredPhotosUC
) {
    val photos = MutableLiveData<List<PhotoEntity>>()


    fun init() {
        getRegisteredPhotosUC.execute()
            .subscribeBy(
                onSuccess = {
                    photos.value = it
                },
                onError = { Log.i(tag(), it.message) })
    }

    fun query(query:String){
        getRegisteredPhotosUC.execute(query)
            .subscribeBy(
                onSuccess = {
                    photos.value = it
                },
                onError = { Log.i(tag(), it.message) })
    }

}