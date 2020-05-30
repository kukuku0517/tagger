package com.project.tagger.tag

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.project.tagger.gallery.TagEntity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.modPositive
import com.project.tagger.util.show
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.dialog_tag_bottom_sheet.*
import kotlinx.android.synthetic.main.item_tag_gallery.view.*
import org.koin.android.ext.android.inject
import java.util.ArrayList

class TagBottomSheetDialog private constructor() : BottomSheetDialogFragment() {
    companion object {
        const val PHOTOS = "PHOTOS"
        const val REPO = "REPO"
        const val TAG = "TAG"

        fun create(
            photos: List<PhotoEntity>,
            repo: RepoEntity,
            tags: List<TagEntity>? = null
        ): TagBottomSheetDialog {
            return TagBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(PHOTOS, photos as ArrayList<out Parcelable>)
                    putParcelable(REPO, repo)
                    tags?.let {
                        putParcelableArrayList(TAG, it as ArrayList<out Parcelable>)
                    }

                }
            }
        }
    }

    private var dismissListener: (() -> Unit)? = null

    fun setOnDismissListener(onDismiss: () -> Unit): TagBottomSheetDialog {
        this.dismissListener = onDismiss
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        dismissListener?.invoke()
        super.onDismiss(dialog)
    }

    val tagViewModel: TagViewModel by inject()
    val tagColors = listOf(
        R.color.tag_color_0,
        R.color.tag_color_1,
        R.color.tag_color_2,
        R.color.tag_color_3
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_tag_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photos: ArrayList<PhotoEntity>? = arguments?.getParcelableArrayList<PhotoEntity>(PHOTOS)
        val repo = arguments?.getParcelable<RepoEntity>(REPO)
        val tags: ArrayList<TagEntity>? = arguments?.getParcelableArrayList<TagEntity>(TAG)
        if (photos == null || repo == null) {
            dismiss()
        }


        val adapter = SimpleRecyclerViewAdapter(
            requireContext(),
            object : SimpleRecyclerViewAdapter.RecyclerProvider<PhotoEntity>() {
                override fun getLayoutId(): Int {
                    return R.layout.item_tag_gallery
                }

                override fun getItemCount(): Int {
                    return items.size
                }

                override fun getItem(position: Int): PhotoEntity {
                    return items.get(position)
                }

                override fun onBindView(containerView: View, item: PhotoEntity) {
                    Log.i("tagGal", "${item.path}")
                    Glide.with(requireContext()).load(item.path)
                        .apply(
                            RequestOptions().centerCrop()
                                .override(200)
                                .fitCenter()
                        ).into(containerView.mIvTagGallery)
                }

                override fun onClick(adapterPosition: Int) {

                }

                override fun setItem(
                    adapter: SimpleRecyclerViewAdapter<PhotoEntity>,
                    oldItems: List<PhotoEntity>,
                    newItems: List<PhotoEntity>
                ) {
                    Log.i("tagGal", "size  ${newItems.size}")
                    adapter.notifyDataSetChanged()
                }
            })
        mRvTagPhotos.adapter = adapter
        mRvTagPhotos.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter.provider.items = photos!!

        tagViewModel.repo = repo

        tagViewModel.setTags(tags)
        tagViewModel.setPhotos(photos)

        tagViewModel.tags.observe(this, Observer { tags ->
            mCgTag.removeAllViews()
            tags.forEach { tag ->
                mCgTag.addView(
                    (layoutInflater.inflate(R.layout.chip_filter_layout, mCgTag, false) as Chip)
                        .apply {
                            chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    tagColors[tag.tag.tag.hashCode().modPositive(tagColors.size)]
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


        tagViewModel.recommendedTags.observe(this, Observer { tags ->
            mCgTagRecommend.removeAllViews()
            tags.forEach { tag ->
                mCgTagRecommend.addView(
                    (layoutInflater.inflate(
                        R.layout.chip_filter_layout,
                        mCgTagRecommend,
                        false
                    ) as Chip)
                        .apply {
                            chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    R.color.grey
                                )
                            )
                            text = tag.tag.tag
                            isCheckable = false
                            setOnClickListener {
                                tagViewModel.addTag(tag.tag.tag)
                            }
                        }
                )
            }
        })


        tagViewModel.finishEvent.observe(this, Observer {
            if (it) {
                dismiss()
            }
        })
        tagViewModel.isLoading.observe(this, Observer {
            mTvTagComplete.visibility = if (it) View.INVISIBLE else View.VISIBLE
            mPbTagLoading.show(it)
        })

        mEtTag.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
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