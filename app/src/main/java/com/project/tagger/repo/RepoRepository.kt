package com.project.tagger.repo

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.*
import com.project.tagger.database.*
import com.project.tagger.database.FirebaseConstants.Companion.REPO
import com.project.tagger.database.FirebaseConstants.Companion.USER
import com.project.tagger.gallery.toEntity
import com.project.tagger.gallery.toPojo
import com.project.tagger.login.UserEntity
import com.project.tagger.util.Gateway
import com.project.tagger.util.asCompletable
import com.project.tagger.util.tag
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.Exception

interface RepoRepository {

    fun postRepos(repoEntity: RepoEntity): Single<RepoEntity>
    fun deleteAllRepo(): Completable
    fun getRepos(user: UserEntity, refresh: Boolean = false): Single<List<RepoEntity>>
    fun findRepo(params: Int?): Maybe<RepoEntity>
    fun updateRepoLocal(repos: RepoEntity): Maybe<RepoEntity>
}

class RepoRepositoryImpl(val context: Context, val appDatabase: AppDatabase) : RepoRepository {
    val db = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        firestoreSettings = settings

    }

//    var repoCache: List<RepoEntity>? = null
//    override fun getRepos(user: UserEntity): Single<List<RepoEntity>> {
//        return if (repoCache != null) {
//            Single.just(repoCache)
//        } else {
//            getReposFromLocal()
//                .switchIfEmpty(getReposFromServer(user).flatMap { updateReposLocal(user, it) })
//                .switchIfEmpty(getDefaultRepos(user).flatMap { updateReposServer(it) })
//                .toSingle()
//        }
//    }


    var repoCache: Gateway<List<RepoEntity>>? = null

    override fun getRepos(user: UserEntity, refresh: Boolean): Single<List<RepoEntity>> {
        if (refresh || true) {
            repoCache?.clearRequest()

        }
        repoCache = repoCache ?: Gateway.from(
            getReposFromLocal()
                .switchIfEmpty(getReposFromServer(user).flatMap { updateReposLocal(user, it) })
                .switchIfEmpty(getDefaultRepos(user).flatMap { updateReposServer(it) })
                .toSingle()
        )
        return repoCache!!
    }

    override fun findRepo(params: Int?): Maybe<RepoEntity> {
        return Maybe.create<RepoEntity> { emitter ->
            db.collection(REPO).whereEqualTo("id", params)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (firebaseFirestoreException != null) {
                        emitter.onError(firebaseFirestoreException)
                        return@addSnapshotListener
                    }

                    if (querySnapshot?.isEmpty == false) {
                        querySnapshot.documents.firstOrNull()?.toObject(RepoEntity::class.java)?.let {
                            emitter.onSuccess(it)
                        } ?: run {
                            emitter.onError(Exception("Parsing error"))
                        }
                    } else {
                        emitter.onComplete()
                    }
                }
        }
    }

    override fun postRepos(repoEntity: RepoEntity): Single<RepoEntity> {
        return updateRepoServer(repoEntity)
            .flatMapMaybe { updateRepoLocal(it) }
            .firstOrError()
    }

    private fun getReposFromServer(user: UserEntity): Maybe<List<RepoEntity>> {
        return Maybe.create<List<RepoEntity>> { emitter ->
            val repos = mutableListOf<RepoEntity>()
            db.runTransaction { transaction ->
                user.repoReferences.forEach {
                    Log.i(tag(), "getReposFromServer ${it}")
                    val repo = transaction.get(db.document(it)).toObject(RepoEntity::class.java)
                    repo?.let { it1 -> repos.add(it1) }
                }
                user.visitorReferences.forEach {
                    Log.i(tag(), "getReposFromServer ${it}")
                    val repo = transaction.get(db.document(it)).toObject(RepoEntity::class.java)
                    repo?.let { it1 -> repos.add(it1) }
                }
                return@runTransaction repos
            }.addOnSuccessListener {
                if (it.isNotEmpty()) {
                    emitter.onSuccess(it)
                } else {
                    emitter.onComplete()
                }
            }.addOnFailureListener {
                emitter.onError(it)
            }
        }
//        return db.collection(USER).document(user.email).collection(REPO).asMaybe()
    }

    private fun getDefaultRepos(user: UserEntity): Maybe<List<RepoEntity>> {

        return Maybe.defer {
            val defaultRepo = RepoEntity(
                owner = user.email,
                visitor = listOf(),
                photos = listOf(),
                name = "${user.name}'s repository",
                desc = "",
                id = "${user.email}${user.name}".hashCode()
            )
            Log.i(tag(), "getDefaultRepos ${defaultRepo.id} ${defaultRepo.name}")

            Maybe.just(listOf(defaultRepo))
        }


    }

    private fun getReposFromLocal(): Maybe<List<RepoEntity>> {
        return Maybe.defer {
            val repos = appDatabase.repoDao().getRepos()
            if (repos.isNotEmpty()) {
                Maybe.just(
                    repos.map {
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
        return db.runTransaction { transaction ->
            val user = transaction.get(db.collection(USER).document(it.owner))
                .toObject(UserEntity::class.java)

            if (user != null) {
                val repoRef = db.collection(REPO).document(it.id.toString())
                transaction.set(repoRef, it, SetOptions.merge())

                val repos = user.repoReferences
                if (repos.firstOrNull { it == repoRef.path } == null) {
                    transaction.set(
                        db.collection(USER).document(it.owner),
                        user.copy(
                            repoReferences = user.repoReferences.toMutableList()
                                .apply { add(repoRef.path) }
                        ), SetOptions.merge()
                    )
                }
            }
        }
            .asCompletable()
            .andThen(Observable.just(it))


//        return db.collection(USER).document(it.owner).collection(REPO).document(it.id.toString())
//            .set(it, SetOptions.merge()).asCompletable()
//            .andThen(updateRepoLocal(it))
//            .toObservable()
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


    override fun updateRepoLocal(repos: RepoEntity): Maybe<RepoEntity> {
        return Maybe.defer {
            Log.i(tag(), "updateRepoLocal ${repos.id} ${repos.name}")
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

    override fun deleteAllRepo(): Completable {
        return Completable.fromAction {
            appDatabase.repoDao().deleteAll()
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
        id = this.repo.id,
        backUp = this.repo.isBackUp
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
        desc = this.desc,
        isBackUp = this.backUp
    )
}

