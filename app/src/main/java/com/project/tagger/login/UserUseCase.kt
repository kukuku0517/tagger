package com.project.tagger.login

import com.project.tagger.util.UseCaseParameterNullPointerException
import com.project.tagger.util.UseCaseSingle
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