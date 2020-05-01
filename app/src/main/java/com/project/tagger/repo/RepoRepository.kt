package com.project.tagger.repo

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tagger.database.AppDatabase
import com.project.tagger.database.FirebaseConstants.Companion.REPO
import com.project.tagger.database.FirebaseConstants.Companion.USER
import com.project.tagger.database.RepoPojo
import com.project.tagger.database.RepoWithPhotosAndVisitors
import com.project.tagger.database.UserPojo
import com.project.tagger.gallery.toEntity
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
}

class RepoRepositoryImpl(val context: Context, val appDatabase: AppDatabase) : RepoRepository {
    val db = FirebaseFirestore.getInstance()

    override fun getRepos(user: UserEntity): Single<List<RepoEntity>> {
        return getReposFromLocal()
            .switchIfEmpty(getReposFromServer(user).flatMap { updateReposLocal(user, it) })
            .switchIfEmpty(getDefaultRepos(user).flatMap { updateReposServer(user, it) })
            .toSingle()
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
        user: UserEntity,
        repos: List<RepoEntity>
    ): Maybe<List<RepoEntity>> {
        return Observable.fromIterable(repos)
            .flatMap {
                db.collection(USER).document(user.email).collection(REPO).document(it.id.toString())
                    .set(it).asCompletable()
                    .andThen(Observable.just(it))
            }
            .toList()
            .toMaybe()
    }

    private fun updateReposLocal(
        user: UserEntity,
        repos: List<RepoEntity>
    ): Maybe<List<RepoEntity>> {
        return Maybe.defer {
            appDatabase.repoDao().updateRepos(repos.map { it.toPojo() })
            Maybe.just(repos)
        }

            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

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

