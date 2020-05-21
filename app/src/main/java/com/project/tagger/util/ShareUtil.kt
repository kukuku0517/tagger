package com.project.tagger.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.content.FileProvider
import com.project.tagger.R
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ShareUtil {
    companion object {

        fun shareImage(context: Context, path: String): Boolean {
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
            return true
        }

        fun shareImage(context: Context, view: View) {
            val type = "image/*";
            val imageFile = createBitmapFromView(context, view)


            val intent = Intent(Intent.ACTION_SEND)
            intent.type = type

            val uri: Uri
            uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    imageFile
                )
            } else {
                Uri.fromFile(imageFile)
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
        }

        private fun createBitmapFromView(context: Context, view: View): File {
            val fileName =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".jpg"

            val bitmap = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
            );
            val c = Canvas(bitmap)
            view.draw(c)
            c.drawBitmap(bitmap, 0f, 0f, null)

            var fout: OutputStream? = null
            val imagePath = File(context.cacheDir, "images")
            val imageFile = File(imagePath, fileName)
            try {
                fout = FileOutputStream(imageFile);

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    80,
                    fout
                );//set image quality and formate as you required.
                fout.flush();
                fout.close();
            } catch (e: FileNotFoundException) {
                e.printStackTrace();
            } catch (e: IOException) {
                e.printStackTrace();
            }
            return imageFile
        }

    }
}