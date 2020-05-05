package com.project.tagger.database

import androidx.room.*
import com.project.tagger.gallery.PhotoEntity

@Entity(tableName = "photo")
data class PhotoPojo(
    @PrimaryKey
    val path: String,
    val remotePath: String?,
    val isDirectory: Boolean,
    val folderName: String?,
    val repoId: Int? = null,

    val createdAt: String = "2020-05-05T00:00:00",
    val updatedAt: String = "2020-05-05T00:00:00",
    val usedAt: String = "2020-05-05T00:00:00",
    val sharedCount: Int = 0

)

@Entity(tableName = "tag")
data class TagPOJO(
    @PrimaryKey

    val tag: String
)


@Entity(primaryKeys = ["path", "tag"])
data class PhotoTagJoin(
    val path: String,
    val tag: String
)

data class PhotoWithTags(
    @Embedded val photos: PhotoPojo,
    @Relation(
        parentColumn = "path",
        entityColumn = "tag",
        associateBy = Junction(PhotoTagJoin::class)
    )
    val tags: List<TagPOJO>
)


data class TagWithPhotos(
    @Embedded val tag: TagPOJO,
    @Relation(
        parentColumn = "tag",
        entityColumn = "path",
        associateBy = Junction(PhotoTagJoin::class)
    )
    val photos: List<PhotoPojo>
)


@Dao
interface PhotoDao {
    @Query("Select * from photo")
    fun getPhotos(): List<PhotoPojo>

    @Transaction
    @Query("SELECT * FROM photo")
    fun getPhotoWithTags(): List<PhotoWithTags>

    @Transaction
    @Query("SELECT * FROM photo where path like :path")
    fun getPhotoWithTagsByPath(path: String): List<PhotoWithTags>

    @Transaction
    @Query("SELECT * FROM photo where path IN (:ids)")
    fun getPhotosWithTagsByIds(ids: List<String>): List<PhotoWithTags>

    @Transaction
    @Query("SELECT * FROM photo where path = :id")
    fun getPhotoWithTagsById(id: String): PhotoWithTags?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdate(toPojo: PhotoPojo)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdateWithTag(toPojo: PhotoTagJoin)

    @Delete
    fun deleteTag(toPojo: PhotoTagJoin)


    @Delete
    fun deleteTags(toPojo: List<PhotoTagJoin>)

    @Delete
    fun deletePhoto(photo: PhotoPojo)
}


@Dao
interface TagDao {
    @Query("Select * from tag")
    fun getTags(): List<TagPOJO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdate(tag: TagPOJO)

    @Transaction
    @Query("SELECT * FROM tag where tag like :query")
    fun getTagWithPhotosByQuery(query: String): List<TagWithPhotos>


    @Transaction
    @Query("SELECT * FROM tag")
    fun getTagWithPhotos(): List<TagWithPhotos>


}
