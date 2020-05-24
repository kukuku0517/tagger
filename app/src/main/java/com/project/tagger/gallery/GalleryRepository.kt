package com.project.tagger.gallery

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.project.tagger.database.*
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.LocalImageFileSaver
import com.project.tagger.util.fromIO
import com.project.tagger.util.tag
import com.project.tagger.util.toUI
import id.zelory.compressor.Compressor
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*


interface GalleryRepository {

    fun createGridItems(params: String): Single<MutableList<GalleryEntity>>
    fun getRegisteredPhotos(query: String?, path: String?): Single<List<PhotoEntity>>
    fun register(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity>
    fun uploadPhoto(repo: RepoEntity, photoEntity: PhotoEntity): Single<PhotoEntity>
    fun getTags(): Single<List<TagEntity>>
    fun registerReplace(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity>
    fun deletePhoto(params: PhotoEntity): Single<PhotoEntity>
    fun deleteRegisteredPhotos(): Completable
}

class LocalGalleryRepository(
    val context: Context,
    val appDatabase: AppDatabase
) : GalleryRepository {
    val storage = FirebaseStorage.getInstance()

    override fun createGridItems(params: String): Single<MutableList<GalleryEntity>> {
        return Single.defer {
            val item = if (params.isEmpty()) {
                getBuckets()
            } else {
                getImagesByBucket(params)
            }
            Single.just(item)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())


    }

    private fun getBuckets(): MutableList<GalleryEntity> {
        val buckets: MutableList<GalleryEntity> = ArrayList()
        val bucketSet: MutableSet<String> = mutableSetOf()
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf<String>(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID
        )

        val cursor: Cursor? = context.getContentResolver().query(uri, projection, null, null, null)
        if (cursor != null) {
            var file: File
            while (cursor.moveToNext()) {
                val bucketName: String =
                    cursor.getString(cursor.getColumnIndex(projection[0]))
                val firstImage: String =
                    cursor.getString(cursor.getColumnIndex(projection[1]))
                val bucketPath: String =
                    cursor.getString(cursor.getColumnIndex(projection[2]))
                file = File(firstImage)
                if (file.exists() && !bucketSet.contains(bucketName)) {
                    val images = file.parentFile?.listFiles(ImageFileFilter()) ?: arrayOf()
                    buckets.add(
                        GalleryEntity(
                            bucketName,
                            true,

                            bucketName,
                            false,
                            images.size,
                            0,
                            firstImage

                        )
                    )
                    bucketSet.add(bucketName)
                }
            }
            cursor.close()
        }
        return buckets
    }

    fun getImagesByBucket(bucketPath: String): MutableList<GalleryEntity> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(MediaStore.Images.Media.DATA)
        val selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?"
        val orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC"
        val images: MutableList<String> = ArrayList()
        val photos: MutableList<GalleryEntity> = mutableListOf()
        val cursor: Cursor? = context.contentResolver
            .query(uri, projection, selection, arrayOf(bucketPath), orderBy)
        if (cursor != null) {
            var file: File
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndex(projection[0]))
                file = File(path)
                if (file.exists() && !images.contains(path)) {
                    images.add(path)
                    photos.add(
                        GalleryEntity(
                            path,
                            false,

                            path,
                            false,
                            0,
                            0,
                            null
                        )
                    )
                }
            }
            cursor.close()
        }
        return photos
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

    override fun deleteRegisteredPhotos(): Completable {
        return Completable.fromAction {
            appDatabase.photoDao().deleteAll()
        }
            .fromIO()
            .toUI()
    }

    override fun uploadPhoto(repo: RepoEntity, photoEntity: PhotoEntity): Single<PhotoEntity> {
        return Single.create<PhotoEntity> { emitter ->
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

//            var file = Uri.fromFile(File(photoEntity.path))
            val compressed = Compressor(context).setMaxWidth(640)
                .setMaxHeight(480)
                .setQuality(75)
                .setCompressFormat(Bitmap.CompressFormat.WEBP)
                .compressToFile(File(photoEntity.path))
            val file = Uri.fromFile(compressed)
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
                        id = photoWithTags.photos.id,
                        tag = it.tag
                    )
                )
            }

            Single.just(photoWithTags.toEntity())
        }
            .doOnSuccess { savePhotoLocal(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun savePhotoLocal(photoEntity: PhotoEntity) {
        val path = ("internal_" + photoEntity.path).replace("/", "")
        val file = File(photoEntity.path)
        LocalImageFileSaver.writeImage(context, path, file)
    }

    override fun registerReplace(repo: RepoEntity, params: PhotoEntity): Single<PhotoEntity> {
        return Single.defer {
            val photoWithTags = params.toPojo()

            appDatabase.photoDao().createOrUpdate(photoWithTags.photos)

            val photo = appDatabase.photoDao().getPhotoWithTagsById(photoWithTags.photos.id)
            photo ?: throw NullPointerException()

            photoWithTags.tags.forEach {
                appDatabase.tagDao().createOrUpdate(it)
            }

            val oldTagJoin = photo.tags.map { PhotoTagJoin(id = photo.photos.id, tag = it.tag) }
            appDatabase.photoDao().deleteTags(oldTagJoin)

            photoWithTags.tags.forEach {
                appDatabase.photoDao().createOrUpdateWithTag(
                    PhotoTagJoin(
                        id = photoWithTags.photos.id,
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
        }
            .fromIO()
            .toUI()
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
        id = this.id,
        path = this.path,
        remotePath = this.remotePath,
        isDirectory = this.directory,
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
            id = this.id,
            path = this.path,
            remotePath = this.remotePath,
            isDirectory = this.directory,
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
        id = this.photos.id,
        path = this.photos.path,
        remotePath = this.photos.remotePath,
        directory = false,
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