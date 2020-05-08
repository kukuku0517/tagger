package com.project.tagger.repo

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.tagger.database.*
import com.project.tagger.database.FirebaseConstants.Companion.REPO
import com.project.tagger.database.FirebaseConstants.Companion.USER
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.toEntity
import com.project.tagger.gallery.toPojo
import com.project.tagger.login.UserEntity
import com.project.tagger.util.asCompletable
import com.project.tagger.util.asMaybe
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

interface RepoRepository {

    fun getRepos(user: UserEntity): Single<List<RepoEntity>>
    fun postRepos(repoEntity: RepoEntity): Single<RepoEntity>
    fun deleteAllRepo()
}

class RepoRepositoryImpl(val context: Context, val appDatabase: AppDatabase) : RepoRepository {
    val db = FirebaseFirestore.getInstance()

    override fun getRepos(user: UserEntity): Single<List<RepoEntity>> {
        return getReposFromLocal()
            .switchIfEmpty(getReposFromServer(user).flatMap { updateReposLocal(user, it) })
            .switchIfEmpty(getDefaultRepos(user).flatMap { updateReposServer(it) })
            .toSingle()
    }

    override fun postRepos(repoEntity: RepoEntity): Single<RepoEntity> {
        return updateRepoServer(repoEntity).firstOrError()
    }

    private fun getDefaultRepos(user: UserEntity): Maybe<List<RepoEntity>> {

        val defaultRepo = RepoEntity(
            owner = user.email,
            visitor = listOf(),
            photos = listOf(),
            name = "${user.name}'s repository",
            desc = "",
            id = "${user.email}${user.name}${System.currentTimeMillis()}".hashCode()
        )

        return Maybe.just(listOf(defaultRepo))

    }

    private fun getReposFromServer(user: UserEntity): Maybe<List<RepoEntity>> {
        return db.collection(USER).document(user.email).collection(REPO).asMaybe()
    }

    private fun getReposFromLocal(): Maybe<List<RepoEntity>> {
        return Maybe.defer {
            val repos = appDatabase.repoDao().getRepos()
            if (repos.isNotEmpty()) {
                Maybe.just(
                    repos.map {
                        it.toEntity()
                        val photos = it.photos.mapNotNull {
                            appDatabase.photoDao().getPhotoWithTagsById(it.path)?.toEntity()
                        }
                        it.toEntity().copy(photos = photos)
                    }
                )
            } else {
                Maybe.empty()
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    private fun updateReposServer(
        repos: List<RepoEntity>
    ): Maybe<List<RepoEntity>> {
        return Observable.fromIterable(repos)
            .flatMap {
                updateRepoServer(it)
            }
            .toList()
            .toMaybe()
    }

    private fun updateRepoServer(it: RepoEntity): Observable<RepoEntity> {
        return db.collection(USER).document(it.owner).collection(REPO).document(it.id.toString())
            .set(it, SetOptions.merge()).asCompletable()
            .andThen(updateRepoLocal(it))
            .toObservable()
    }

    private fun updateReposLocal(
        user: UserEntity,
        repos: List<RepoEntity>
    ): Maybe<List<RepoEntity>> {
        return Observable.fromIterable(repos)
            .flatMapMaybe { updateRepoLocal(it) }
            .toList()
            .toMaybe()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    private fun updateRepoLocal(repos: RepoEntity): Maybe<RepoEntity> {
        return Maybe.defer {
            appDatabase.repoDao().updateRepos(listOf(repos.toPojo()))
            updateRepoDetailLocal(repos)
            Maybe.just(repos)

        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun updateRepoDetailLocal(repo: RepoEntity) {
        repo.photos.forEach {
            val photoWithTags = it.toPojo()
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
        }


    }

    override fun deleteAllRepo(){
        appDatabase.repoDao().deleteAll()
    }
}


fun RepoWithPhotosAndVisitors.toEntity(): RepoEntity {
    return RepoEntity(
        owner = this.repo.owner,
        visitor = this.visitor.mapNotNull { it.toEntity().name },
//            photos = this.photos.map { it.toEntity() },
        photos = listOf(),
        name = this.repo.name,
        desc = this.repo.desc,
        id = this.repo.id
    )
}

fun UserPojo.toEntity(): UserEntity {
    return UserEntity(
        name = this.name,
        email = this.email,
        profileUrl = this.profileUrl
    )
}

fun RepoEntity.toPojo(): RepoPojo {
    return RepoPojo(
        id = this.id,
        owner = this.owner,
        name = this.name,
        desc = this.desc
    )
}

