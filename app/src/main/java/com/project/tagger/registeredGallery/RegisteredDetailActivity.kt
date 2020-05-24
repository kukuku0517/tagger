package com.project.tagger.registeredGallery

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.chip.Chip
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.gallery.TagEntity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.ShareUtil
import com.project.tagger.util.show
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_registered_detail.*
import kotlinx.android.synthetic.main.item_tag_gallery.view.*
import org.koin.android.ext.android.inject

class RegisteredDetailActivity : AppCompatActivity() {
    val tagColors = listOf(
        R.color.tag_color_0,
        R.color.tag_color_1,
        R.color.tag_color_2,
        R.color.tag_color_3
    )
    val tagViewModel: RegisteredDetailViewModel by inject()

    companion object {
        const val PHOTOS = "PHOTOS"
        const val REPO = "REPO"
        const val TAG = "TAG"

        fun create(
            context: Context,
            photos: PhotoEntity,
            repo: RepoEntity,
            tags: List<TagEntity>? = null
        ) {
            context.startActivity(Intent(context, RegisteredDetailActivity::class.java).apply {
                putExtra(PHOTOS, photos)
                putExtra(REPO, repo)
                tags?.let {
                    putParcelableArrayListExtra(TAG, it as ArrayList<out Parcelable>)
                }
            })

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registered_detail)

        val photos: PhotoEntity? = intent.extras?.getParcelable(PHOTOS)
        val repo = intent.extras?.getParcelable<RepoEntity>(REPO)
        val tags: ArrayList<TagEntity>? = intent.extras?.getParcelableArrayList<TagEntity>(TAG)
        if (photos == null || repo == null) {
            finish()
            return
        }

        tagViewModel.repo = repo
        mIvRegDetailShare.setOnClickListener {
//            ShareUtil.shareImage(this, photos.path)
//            ShareUtil.shareImage(this, mIvRegDetail)

            ShareUtil.shareImage(this, "${filesDir.path}/${photos.parseLocalPath()}")
        }

        mIvRegDetailDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Confirm") { dialog, which ->
                    tagViewModel.delete()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }.show()
        }

        tagViewModel.setTags(tags)
        tagViewModel.photos = photos
        Glide.with(this).load(photos.path)
            .error(Glide.with(this).load(photos.remotePath))
            .apply(
                RequestOptions().fitCenter()
            ).into(mIvRegDetail)
        tagViewModel.tags.observe(this, Observer { tags ->
            mCgRegDetail.removeAllViews()
            tags.forEach { tag ->
                mCgRegDetail.addView(
                    (layoutInflater.inflate(
                        R.layout.chip_filter_layout,
                        mCgRegDetail,
                        false
                    ) as Chip)
                        .apply {
                            chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    tagColors[tag.hashCode() % tagColors.size]
                                )
                            )
                            text = tag.tag.tag
                            Log.i(tag(), "isChecked ${tag.isSelected}")
                            isChecked = tag.isSelected
                            setOnCheckedChangeListener { buttonView, isChecked ->
                                if (isChecked) {
                                    tagViewModel.addTag(tag.tag.tag)
                                } else {
                                    tagViewModel.removeTag(tag.tag.tag)

                                }
                            }
                        }
                )
            }
        })
        tagViewModel.finishEvent.observe(this, Observer {
            if (it) {
                finish()
            }
        })
        tagViewModel.isLoading.observe(this, Observer {
            mTvRegDetailComplete.visibility = if (it) View.INVISIBLE else View.VISIBLE
            mPbRegDetailLoading.show(it)
        })

        mEtRegDetail.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val text = mEtRegDetail.text.toString()
                    if (text.isBlank()){
                        Toast.makeText(this, getString(R.string.tag_empty_message),Toast.LENGTH_LONG).show()
                    }else{
                        tagViewModel.addTag(text)
                        mEtRegDetail.text = null
                    }
                }
            }

            true
        }

        mTvRegDetailComplete.setOnClickListener {
            tagViewModel.updateTags()
        }
    }
}
