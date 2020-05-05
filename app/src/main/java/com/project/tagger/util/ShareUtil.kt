package com.project.tagger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.project.tagger.R
import java.io.File

class ShareUtil {
    companion object{

        fun shareImage(context: Context, path: String) {
            val type = "image/*";
            val imageFile = File(path)


            val intent = Intent(Intent.ACTION_SEND)
            intent.type = type

            val uri: Uri
            uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    imageFile
                )
            } else {
                Uri.fromFile(imageFile)
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
        }
    }
}