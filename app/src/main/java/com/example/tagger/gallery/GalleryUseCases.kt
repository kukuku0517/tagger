package com.example.tagger.gallery

import android.os.Parcelable
import com.example.tagger.util.UseCaseParameterNullPointerException
import com.example.tagger.util.UseCaseSingle
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize


@Parcelize
data class PhotoEntity(
    val path: String,
    val remotePath: String?,
    val tags: List<TagEntity> = listOf(),
    val isDirectory: Boolean,
    val folderName: String?,
    val isRegistered: Boolean = false
) : Parcelable

@Parcelize
data class TagEntity(
    val tag: String
) : Parcelable

class GetPhotosFromGalleryUC(
    val photoRepository: GalleryRepository
) : UseCaseSingle<String, List<PhotoEntity>> {
    override fun execute(params: String?): Single<List<PhotoEntity>> {
        params ?: return Single.error(UseCaseParameterNullPointerException())
        return Single.just(photoRepository.createGridItems(params))
            .flatMap { photoFromGallery ->
                photoRepository.getRegisteredPhotos(query = null, path = params)
                    .map { registeredPhotos ->
                        photoFromGallery.map {
                            if (registeredPhotos.map { it.path }.contains(it.path)) {
                                it.copy(
                                    isRegistered = true
                                )
                            } else {
                                it
                            }
                        }
                    }
            }
    }
}

class GetRegisteredPhotosUC(val photoRepository: GalleryRepository) :
    UseCaseSingle<String, List<PhotoEntity>> {
    override fun execute(query: String?): Single<List<PhotoEntity>> {
        return photoRepository.getRegisteredPhotos(query = query, path = null)
            .map { it.sortedByDescending { it.tags.size } }
    }
}

class RegisterPhotoUc(val photoRepository: GalleryRepository) :
    UseCaseSingle<PhotoEntity, PhotoEntity> {
    override fun execute(params: PhotoEntity?): Single<PhotoEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        return photoRepository.register(params)
    }
}

class RegisterTagsOnPhotosUC(val photoRepository: GalleryRepository) :
    UseCaseSingle<Pair<List<TagEntity>, List<PhotoEntity>>, List<PhotoEntity>> {
    override fun execute(params: Pair<List<TagEntity>, List<PhotoEntity>>?): Single<List<PhotoEntity>> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        val tags = params.first
        val photos = params.second
        val taggedPhotos =
            photos.map { it.copy(tags = it.tags.toMutableSet().apply { addAll(tags) }.toList()) }
        return Observable.fromIterable(taggedPhotos)
            .flatMapSingle { photoRepository.register(it) }
            .toList()
    }

}