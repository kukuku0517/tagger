package com.project.tagger.repo

import android.os.Parcelable
import android.util.Log
import androidx.room.Ignore
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.UserRepository
import com.project.tagger.util.UseCaseMaybe
import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
import com.project.tagger.util.tag
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import java.lang.NullPointerException

@Parcelize
@IgnoreExtraProperties
data class RepoEntity(
    val id: Int = -1,
    val owner: String = "",
    val visitor: List<String> = listOf(),
    val photos: List<PhotoEntity> = listOf(),
    val name: String = "",
    val desc: String = "",
    val thumb: String = "",
    @SerializedName("isBackUp", alternate = ["backUp"])
    @field:JvmField
    val backUp: Boolean = false
) : Parcelable {
    @Ignore
    @Exclude
    fun parseToRepoPath(): String {
        return "REPO/${id}"
    }
}


class GetReposUC(
    val repoRepository: RepoRepository,
    val getUserUC: GetUserUC
) : UseCaseSingle<Void, List<RepoEntity>> {
    override fun execute(params: Void?): Single<List<RepoEntity>> {
        return getUserUC.execute()
            .flatMapSingle { user ->
                repoRepository.getRepos(user)
            }
            .doOnError { Log.w(tag(), it.message) }
    }
}

class PostRepoUC(val repoRepository: RepoRepository) : UseCaseSingle<RepoEntity, RepoEntity> {
    override fun execute(params: RepoEntity?): Single<RepoEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())
        return repoRepository.postRepos(params)
    }

}

class AddRepoUC(
    val repoRepository: RepoRepository,
    val userRepository: UserRepository
) : UseCaseSingle<RepoEntity, RepoEntity> {
    override fun execute(params: RepoEntity?): Single<RepoEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())
        return Single.defer {
            val user = userRepository.getUser()
            user ?: return@defer Single.error<RepoEntity>(NullPointerException())

            val newUser =
                user.copy(visitorReferences = user.visitorReferences.toMutableSet().apply {
                    add(params.parseToRepoPath())
                }.toList())
            userRepository.updateUser(newUser)
                .andThen(Single.just(params))
        }
            .flatMapMaybe { repoRepository.updateRepoLocal(it) }
            .toSingle()
    }
}

class FindRepoUC(
    val repoRepository: RepoRepository
) : UseCaseMaybe<Int, RepoEntity> {
    override fun execute(params: Int?): Maybe<RepoEntity> {
        params ?: return Maybe.error(UseCaseParameterNullPointerException())
        return repoRepository.findRepo(params)
    }
}