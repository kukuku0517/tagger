package com.project.tagger.login

import com.project.tagger.repo.RepoRepository
import com.project.tagger.util.UseCaseCompletable
import com.project.tagger.util.UseCaseMaybe
import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

data class UserEntity(
    val name: String? = "",
    val email: String = "",
    val profileUrl: String? = ""
)

class UpdateUserUC(val userRepository: UserRepository) : UseCaseSingle<UserEntity, UserEntity> {
    override fun execute(params: UserEntity?): Single<UserEntity> {
        params ?: return Single.error(UseCaseParameterNullPointerException())
        return userRepository.updateUser(params)
            .andThen(Single.just(params))
    }
}

class GetUserUC(val userRepository: UserRepository) : UseCaseMaybe<Void, UserEntity> {
    override fun execute(params: Void?): Maybe<UserEntity> {
        return userRepository.getUser()?.let {
            Maybe.just(it)
        } ?: Maybe.empty()
    }

}


class SignOutUC(
    val userRepository: UserRepository,
    val repoRepository: RepoRepository
) : UseCaseCompletable<Void> {
    override fun execute(params: Void?): Completable {
        return userRepository.signOut()
            .andThen { repoRepository.deleteAllRepo() }
    }
}