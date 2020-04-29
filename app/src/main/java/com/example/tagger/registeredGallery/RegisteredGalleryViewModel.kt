package com.example.tagger.registeredGallery

import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.tagger.gallery.GetRegisteredPhotosUC
import com.example.tagger.gallery.PhotoEntity
import com.example.tagger.util.peekIfNotEmpty
import com.example.tagger.util.tag
import io.reactivex.rxkotlin.subscribeBy
import java.util.*
import kotlin.math.log
import kotlin.math.min

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