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
    val repoId: Int? = null
) : Parcelable

@Parcelize
data class TagEntity(
    val tag: String = "",
    val count: Int
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


class RegisterTagsOnPhotosUC(
    val photoRepository: GalleryRepository,
    val repoRepository: RepoRepository
) :
    UseCaseSingle<RegisterTagsOnPhotosUC.RegisterTagParam, List<PhotoEntity>> {

    data class RegisterTagParam(
        val tags: List<TagEntity>,
        val photos: List<PhotoEntity>,
        val repo: RepoEntity
    )

    override fun execute(params: RegisterTagParam?): Single<List<PhotoEntity>> {
        params ?: return Single.error(UseCaseParameterNullPointerException())

        val tags = params.tags
        val photos = params.photos
        val repo = params.repo

        if (tags.isEmpty()) {
            return Single.error(Exception("At least 1 tag is required"))
        }

        val taggedPhotos =
            photos.map {
                it.copy(
                    repoId = repo.id,
                    tags = it.tags.toMutableSet().apply { addAll(tags) }.toList()
                )
            }

        val isBackUp = repo.isBackUp

        return Observable.fromIterable(taggedPhotos)
            .flatMapSingle {
                if (true && it.remotePath.isNullOrEmpty()) {
                    photoRepository.uploadPhoto(repo, it)
                } else {
                    Single.just(it)
                }
            }
            .flatMapSingle { photoRepository.register(repo, it) }
            .toList()
            .flatMap { photos ->
                if (true) {
                    repoRepository.postRepos(
                        repo.copy(photos = repo.photos.toMutableList().apply { addAll(photos) })
                    ).map { photos }
                } else {
                    Single.just(photos)
                }
            }

    }

}

class GetPopularTagsUC(val galleryRepository: GalleryRepository) :
    UseCaseSingle<Void, List<TagEntity>> {
    override fun execute(params: Void?): Single<List<TagEntity>> {
        return galleryRepository.getTags()
            .map { it.sortedByDescending { it.count } }
    }
}