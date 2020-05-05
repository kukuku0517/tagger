package com.project.tagger.repo

import android.os.Parcelable
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.login.GetUserUC
import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RepoEntity(
    val id: Int = -1,
    val owner: String = "",
    val visitor: List<String> = listOf(),
    val photos: List<PhotoEntity> = listOf(),
    val name: String = "",
    val desc: String = "",
    val isBackUp: Boolean = false
) : Parcelable


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