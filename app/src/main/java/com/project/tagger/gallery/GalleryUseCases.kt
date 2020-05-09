package com.project.tagger.gallery

import android.os.Parcelable
import com.project.tagger.repo.RepoEntity
import com.project.tagger.repo.RepoRepository
import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import java.lang.Exception


@Parcelize
data class PhotoEntity(
    val path: String = "",
    val remotePath: String? = "",
    val tags: List<TagEntity> = listOf(),
    val isDirectory: Boolean = false,
    val folderName: String? = "",
    val isRegistered: Boolean = false,
    val repoId: Int? = null,

    val createdAt: String = "2020-05-05T00:00:00",
    val updatedAt: String = "2020-05-05T00:00:00",
    val usedAt: String = "2020-05-05T00:00:00",
    val sharedCount: Int = 0

) : Parcelable

data class GalleryEntity(
    val path: String = "",
    val isDirectory: Boolean = false,
    val folderName: String? = "",
    val isRegistered: Boolean = false,
    val childCount: Int = 0,
    val registeredCount: Int = 0,
    val thumb: String?
)

@Parcelize
data class TagEntity(
    val tag: String = "",
    val count: Int = -1
) : Parcelable

class GetPhotosFromGalleryUC(
    val photoRepository: GalleryRepository
) : UseCaseSingle<String, List<GalleryEntity>> {
    override fun execute(params: String?): Single<List<GalleryEntity>> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        return photoRepository.createGridItems(params)
            .flatMap { photoFromGallery ->
                if (params.isNotEmpty()) {
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
                } else {
                    Observable.fromIterable(photoFromGallery)
                        .flatMapSingle { gallery ->
                            photoRepository.getRegisteredPhotos(query = null, path = gallery.path)
                                .map { registeredPhotos ->
                                    gallery.copy(
                                        registeredCount = registeredPhotos.size
                                    )
                                }
                        }
                        .toList()
                        .map {
                            it.sortedWith(Comparator { o1, o2 ->
                                when {
                                    o1.childCount != o2.childCount -> {
                                        o2.childCount - o1.childCount
                                    }
                                    o1.path != o2.path -> {
                                        o1.path.compareTo(o2.path)
                                    }
                                    else -> {
                                        0
                                    }
                                }
                            })
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


class RegisterTagsOnPhotosUC(
    val photoRepository: GalleryRepository,
    val repoRepository: RepoRepository
) :
    UseCaseSingle<RegisterTagsOnPhotosUC.RegisterTagParam, List<PhotoEntity>> {

    enum class RegisterType {
        MERGE,
        REPLACE
    }

    data class RegisterTagParam(
        val tags: List<TagEntity>,
        val photos: List<PhotoEntity>,
        val repo: RepoEntity,
        val type: RegisterType = RegisterType.MERGE,
        val updatedAt: String
    )

    override fun execute(params: RegisterTagParam?): Single<List<PhotoEntity>> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        val tags = params.tags
        val photos = params.photos
        val repo = params.repo
        val isBackUp = repo.backUp
        if (tags.isEmpty()) {
            return Single.error(Exception("At least 1 tag is required"))
        }

        val taggedPhotos =
            when (params.type) {
                RegisterType.MERGE -> photos.map {
                    it.copy(
                        repoId = repo.id,
                        tags = it.tags.toMutableSet().apply { addAll(tags) }.toList(),
                        updatedAt = params.updatedAt
                    )
                }
                RegisterType.REPLACE -> photos.map {
                    it.copy(
                        repoId = repo.id,
                        tags = tags,
                        updatedAt = params.updatedAt
                    )
                }
            }




        return Observable.fromIterable(taggedPhotos)
            .flatMapSingle {
                if (isBackUp && it.remotePath.isNullOrEmpty()) {
                    photoRepository.uploadPhoto(repo, it)
                } else {
                    Single.just(it)
                }
            }
            .flatMapSingle {
                when (params.type) {
                    RegisterType.MERGE -> photoRepository.register(repo, it)
                    RegisterType.REPLACE -> photoRepository.registerReplace(repo, it)
                }
            }
            .toList()
            .flatMap { photos ->
                if (isBackUp) {
                    repoRepository.postRepos(
                        repo.copy(photos = repo.photos.toMutableList().apply { addAll(photos) })
                    ).map { photos }
                } else {
                    Single.just(photos)
                }
            }

    }

}

class DeletePhotoUC(val galleryRepository: GalleryRepository) :
    UseCaseSingle<PhotoEntity, PhotoEntity> {
    override fun execute(params: PhotoEntity?): Single<PhotoEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        return galleryRepository.deletePhoto(params)
    }

}

class GetPopularTagsUC(val galleryRepository: GalleryRepository) :
    UseCaseSingle<Void, List<TagEntity>> {
    override fun execute(params: Void?): Single<List<TagEntity>> {
        return galleryRepository.getTags()
            .map { it.sortedByDescending { it.count } }
    }
}