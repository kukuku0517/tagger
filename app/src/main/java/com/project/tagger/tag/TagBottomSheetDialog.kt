package com.project.tagger.tag

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.project.tagger.repo.RepoEntity
import kotlinx.android.synthetic.main.dialog_tag_bottom_sheet.*
import org.koin.android.ext.android.inject
import java.util.ArrayList

class TagBottomSheetDialog private constructor() : BottomSheetDialogFragment() {
    companion object {
        const val PHOTOS = "PHOTOS"
        const val REPO = "REPO"
        fun create(photos: List<PhotoEntity>, repo:RepoEntity): TagBottomSheetDialog {
           return TagBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(PHOTOS, photos as ArrayList<out Parcelable>)
                    putParcelable(REPO, repo)
                }
            }
        }
    }

    val tagViewModel: TagViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_tag_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photos: ArrayList<PhotoEntity>? = arguments?.getParcelableArrayList<PhotoEntity>(
            PHOTOS
        )
        val repo = arguments?.getParcelable<RepoEntity>(REPO)
        if (photos == null || repo == null ) {
            dismiss()
        }

        tagViewModel.repo = repo

        tagViewModel.setPhotos(photos)

        tagViewModel.tags.observe(this, Observer {tags ->
            mCgTag.removeAllViews()
            tags.forEach {
                mCgTag.addView(
                    Chip(context).apply { text = it.tag}
                )
            }
        })
        tagViewModel.finishEvent.observe(this, Observer {
            if (it){
                dismiss()
            }
        })

        mEtTag.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    tagViewModel.addTag(mEtTag.text.toString())
                    mEtTag.text = null
                }
            }

            true
        }

        mTvTagComplete.setOnClickListener {
            tagViewModel.updateTags()
        }
    }
}