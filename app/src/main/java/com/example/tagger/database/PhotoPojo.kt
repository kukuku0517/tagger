package com.example.tagger.database

import androidx.room.*
import com.example.tagger.gallery.TagEntity

@Entity(tableName = "photo")
data class PhotoPojo(
    @PrimaryKey
    val path: String,
    val remotePath: String?,
    val isDirectory: Boolean,
    val folderName: String?
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
    @Query("SELECT * FROM photo where path IN (:ids)")
    fun getPhotoWithTagsById(ids:List<String>): List<PhotoWithTags>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdate(toPojo: PhotoPojo)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdateWithTag(toPojo: PhotoTagJoin)
}


@Dao
interface TagDao {
    @Query("Select * from tag")
    fun getTags(): List<TagPOJO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdate(tag: TagPOJO)

    @Transaction
    @Query("SELECT * FROM tag where tag like :query")
    fun getTagWithPhotos(query:String): List<TagWithPhotos>


}
