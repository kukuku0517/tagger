package com.project.tagger.repo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.UserRepository
import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import java.lang.NullPointerException

@Parcelize
data class RepoEntity(
    val id: Int = -1,
    val owner: String = "",
    val visitor: List<String> = listOf(),
    val photos: List<PhotoEntity> = listOf(),
    val name: String = "",
    val desc: String = "",
    @SerializedName("isBackUp", alternate = ["backUp"])
    @field:JvmField
    val backUp: Boolean = false
) : Parcelable {
    fun getPath(): String {
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
) : UseCaseSingle<Int, RepoEntity> {
    override fun execute(params: Int?): Single<RepoEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())
        return repoRepository.addRepo(params)
            .flatMap { repo ->
                val user = userRepository.getUser()
                user ?: return@flatMap Single.error<RepoEntity>(NullPointerException())

                val newUser =
                    user.copy(visitorReferences = user.visitorReferences.toMutableSet().apply {
                        add(repo.getPath())
                    }.toList())
                userRepository.updateUser(newUser)
                    .andThen(Single.just(repo))
            }
    }

}