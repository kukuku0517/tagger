package com.project.tagger.gallery

import android.content.Context
import android.util.Log
import com.project.tagger.database.*
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileFilter


interface GalleryRepository {

    fun createGridItems(params: String): List<PhotoEntity>
    fun getRegisteredPhotos(query: String?, path: String?): Single<List<PhotoEntity>>
    fun register(params: PhotoEntity): Single<PhotoEntity>
}

class LocalGalleryRepository(
    val context: Context,
    val appDatabase: AppDatabase
) : GalleryRepository {

    override fun createGridItems(params: String): List<PhotoEntity> {
        val items: MutableList<GridViewItem> = ArrayList<GridViewItem>()
        Log.i(tag(), "open file $params")
        val files: Array<File> = File(params)
            .listFiles(ImageFileFilter())
        for (file in files) { // Add the directories containing images or sub-directories
            if (file.isDirectory) {
                if (file.listFiles(ImageFileFilter()).isNotEmpty()) {

                    Log.i(tag(), "dir ${file.path}")

                    items.add(
                        GridViewItem(
                            path = file.getAbsolutePath(),
                            folderName = file.absolutePath.split("/").last(),
                            isDirectory = true
                        )
                    )
                } else {
                    Log.i(tag(), "dir ${file.path} empty")

                }

            } else {
                Log.i(tag(), "img ${file.path}")

                items.add(
                    GridViewItem(
                        path = file.getAbsolutePath(),
                        folderName = null,
                        isDirectory = false
                    )
                )
            }
        }
        return items.map { PhotoEntity(it.path, null, listOf(), it.isDirectory, it.folderName) }
    }

    override fun getRegisteredPhotos(query: String?, path: String?): Single<List<PhotoEntity>> {
        return Single.defer {
            when {
                query != null -> Single.just(appDatabase.tagDao().getTagWithPhotos("%${query}%"))
                    .flatMap { tags ->
                        val paths = tags.flatMap { it.photos }.map { it.path }
                        Single.just(appDatabase.photoDao().getPhotoWithTagsById(paths))
                            .map { it.filter { it.tags.isNotEmpty() }.map { it.toEntity() } }
                    }
                path != null -> {
                    Single.just(appDatabase.photoDao().getPhotoWithTagsByPath("%${path}%"))
                        .map { it.filter { it.tags.isNotEmpty() }.map { it.toEntity() } }
                }
                else -> {
                    Single.just(appDatabase.photoDao().getPhotoWithTags())
                        .map { it.filter { it.tags.isNotEmpty() }.map { it.toEntity() } }
                }
            }

        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    override fun register(params: PhotoEntity): Single<PhotoEntity> {
        return Single.defer {
            val photoWithTags = params.toPojo()
            appDatabase.photoDao().createOrUpdate(photoWithTags.photos)
            photoWithTags.tags.forEach {
                appDatabase.tagDao().createOrUpdate(it)
            }
            photoWithTags.tags.forEach {
                appDatabase.photoDao().createOrUpdateWithTag(
                    PhotoTagJoin(
                        path = photoWithTags.photos.path,
                        tag = it.tag
                    )
                )
            }
            Single.just(photoWithTags.toEntity())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())


    }

    fun PhotoEntity.toPojo(): PhotoWithTags {
        return PhotoWithTags(
            photos = PhotoPojo(
                path = this.path,
                remotePath = this.remotePath,
                isDirectory = this.isDirectory,
                folderName = this.folderName

            ), tags = this.tags.map { it.toPojo() }
        )
    }

    fun TagEntity.toPojo(): TagPOJO {
        return TagPOJO(tag = this.tag)
    }

    fun PhotoWithTags.toEntity(): PhotoEntity {
        return PhotoEntity(
            path = this.photos.path,
            remotePath = this.photos.remotePath,
            isDirectory = false,
            folderName = null,
            tags = this.tags.map { it.toEntity() }
        )
    }

    fun TagPOJO.toEntity(): TagEntity {
        return TagEntity(this.tag)
    }

    private class ImageFileFilter : FileFilter {
        override fun accept(file: File): Boolean {
            if (file.isDirectory) {
                return true
            } else if (isImageFile(file.absolutePath)) {
                return true
            }
            return false
        }

        private fun isImageFile(filePath: String): Boolean {
            return filePath.endsWith(".jpg") || filePath.endsWith(".png")
        }
    }


}

data class GridViewItem(
    val path: String,
    val folderName: String? = null,
    val isDirectory: Boolean
)
