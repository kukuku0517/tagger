package com.project.tagger.gallery

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.project.tagger.database.*
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileFilter
import java.lang.Exception
import java.lang.NullPointerException


interface GalleryRepository {

    fun createGridItems(params: String): List<PhotoEntity>
    fun getRegisteredPhotos(query: String?, path: String?): Single<List<PhotoEntity>>
    fun register(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity>
    fun uploadPhoto(repo: RepoEntity, photoEntity: PhotoEntity): Single<PhotoEntity>
    fun getTags(): Single<List<TagEntity>>
    fun registerReplace(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity>
    fun deletePhoto(params: PhotoEntity): Single<PhotoEntity>
}

class LocalGalleryRepository(
    val context: Context,
    val appDatabase: AppDatabase
) : GalleryRepository {
    val storage = FirebaseStorage.getInstance()

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
        return items.map {
            PhotoEntity(
                it.path,
                null,
                listOf(),
                it.isDirectory,
                it.folderName,
                repoId = null
            )
        }
    }

    override fun getRegisteredPhotos(query: String?, path: String?): Single<List<PhotoEntity>> {
        return Single.defer {
            when {
                query != null -> Single.just(appDatabase.tagDao().getTagWithPhotosByQuery("%${query}%"))
                    .flatMap { tags ->
                        val paths = tags.flatMap { it.photos }.map { it.path }
                        Single.just(appDatabase.photoDao().getPhotosWithTagsByIds(paths))
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

    override fun uploadPhoto(repo: RepoEntity, photoEntity: PhotoEntity): Single<PhotoEntity> {
        return Single.create<PhotoEntity> { emitter ->
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

            var file = Uri.fromFile(File(photoEntity.path))
            val fileRef = storageRef.child("images/${repo.owner}/${file.lastPathSegment}")
            val uploadTask = fileRef.putFile(file)

            val urlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        emitter.onError(it)
                    } ?: run {
                        emitter.onError(Exception("Unknown firebase storage exception"))
                    }
                }
                fileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val photoEntityWithRemote =
                        photoEntity.copy(remotePath = downloadUri.toString())
                    emitter.onSuccess(photoEntityWithRemote)
                } else {
                    emitter.onError(Exception("Unknown firebase storage exception"))
                }
            }
        }
    }

    override fun register(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity> {
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
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun registerReplace(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity> {
        return Single.defer {
            val photoWithTags = params.toPojo()

            appDatabase.photoDao().createOrUpdate(photoWithTags.photos)

            val photo = appDatabase.photoDao().getPhotoWithTagsById(photoWithTags.photos.path)
            photo ?: throw NullPointerException()

            photoWithTags.tags.forEach {
                appDatabase.tagDao().createOrUpdate(it)
            }

            val oldTagJoin = photo.tags.map { PhotoTagJoin(path = photo.photos.path, tag = it.tag) }
            appDatabase.photoDao().deleteTags(oldTagJoin)

            photoWithTags.tags.forEach {
                appDatabase.photoDao().createOrUpdateWithTag(
                    PhotoTagJoin(
                        path = photoWithTags.photos.path,
                        tag = it.tag
                    )
                )
            }

            Single.just(photoWithTags.toEntity())
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun deletePhoto(params: PhotoEntity): Single<PhotoEntity> {
        return Single.defer {
            appDatabase.photoDao().deletePhoto(params.toPhotoPojo())
            Single.just(params)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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

    override fun getTags(): Single<List<TagEntity>> {
        return Single.defer {
            val tags = appDatabase.tagDao().getTagWithPhotos()
            Single.just(tags.map { it.toEntity() })
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }


}

data class GridViewItem(
    val path: String,
    val folderName: String? = null,
    val isDirectory: Boolean
)

fun PhotoEntity.toPhotoPojo(): PhotoPojo {
    return PhotoPojo(
        path = this.path,
        remotePath = this.remotePath,
        isDirectory = this.isDirectory,
        folderName = this.folderName,
        repoId = this.repoId,

        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        usedAt = this.usedAt,
        sharedCount = this.sharedCount
    )
}

fun PhotoEntity.toPojo(): PhotoWithTags {
    return PhotoWithTags(
        photos = PhotoPojo(
            path = this.path,
            remotePath = this.remotePath,
            isDirectory = this.isDirectory,
            folderName = this.folderName,
            repoId = this.repoId,

            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            usedAt = this.usedAt,
            sharedCount = this.sharedCount
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
        tags = this.tags.map { it.toEntity() },
        repoId = this.photos.repoId,

        createdAt = this.photos.createdAt,
        updatedAt = this.photos.updatedAt,
        usedAt = this.photos.usedAt,
        sharedCount = this.photos.sharedCount
    )
}

fun TagPOJO.toEntity(): TagEntity {
    return TagEntity(this.tag, -1)
}

fun TagWithPhotos.toEntity(): TagEntity {
    return TagEntity(
        tag = this.tag.tag,
        count = this.photos.size
    )
}