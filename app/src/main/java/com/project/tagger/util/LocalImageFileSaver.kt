package com.project.tagger.util

import android.R.attr.resource
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream


class LocalImageFileSaver {
    companion object{
        fun writeImage(context:Context, path:String, file: File){
            var fos: FileOutputStream? = null
            try {
                fos = context.openFileOutput(path, Context.MODE_PRIVATE)
                fos.write(file.readBytes())
                fos.close()
                Log.i(tag(), "write ${path}")
            } catch (e:Exception){
                Log.i(tag(), "write fail ${e.message}")
            }
        }

        fun writeBitmap(context:Context, path:String, bitmap: Bitmap){
            var fos: FileOutputStream? = null
            try {
                fos = context.openFileOutput(path, Context.MODE_PRIVATE)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
}