package com.project.tagger.database

import androidx.room.*
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.login.UserEntity
import com.project.tagger.repo.RepoRepository


@Entity(tableName = "user")
data class UserPojo(
    val name: String? = "",
    @PrimaryKey
    val email: String = "",
    val profileUrl: String? = ""
)

@Entity(tableName = "repo")
data class RepoPojo(
    @PrimaryKey
    val id: Int,
    val owner: String,
    val name: String,
    val desc: String,
    val isBackUp: Boolean = false,
    val thumb:String

)

data class RepoWithPhotosAndVisitors(
    @Embedded val repo: RepoPojo,
    @Relation(
        parentColumn = "id",
        entityColumn = "repoId"
    )
    val photos: List<PhotoPojo>,
    @Relation(
        parentColumn = "id",
        entityColumn = "email",
        associateBy = Junction(RepoUserJoin::class)
    )
    val visitor: List<UserPojo>
)

@Entity(primaryKeys = ["id", "email"])
data class RepoUserJoin(
    val id: String,
    val email: String
)


@Dao
interface RepoDao {

    //    @Query("Select * from repo")
//    fun get():List<RepoPojo>
    @Transaction
    @Query("Select * from repo")
    fun getRepos(): List<RepoWithPhotosAndVisitors>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateRepos(repos: List<RepoPojo>)

    @Query("Delete from repo")
    fun deleteAll()
}