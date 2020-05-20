package com.project.tagger.gallery

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.project.tagger.R
import com.project.tagger.main.MainPagerAdapter
import com.project.tagger.tag.TagBottomSheetDialog
import com.project.tagger.util.show
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.android.synthetic.main.item_gallery.view.mIvGallery
import kotlinx.android.synthetic.main.item_gallery.view.mTintGallery
import kotlinx.android.synthetic.main.item_gallery.view.mTvGalleryFolderName
import kotlinx.android.synthetic.main.item_gallery.view.mTvGallerySelected
import kotlinx.android.synthetic.main.item_gallery_flat.view.*
import org.koin.android.ext.android.inject
import kotlin.math.max

class GalleryFragment : Fragment(), MainPagerAdapter.FragmentBackPressListener {
    val galleryViewModel: GalleryViewModel by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(tag(), "onCreateView")
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    lateinit var adapter: SimpleRecyclerViewAdapter<GalleryViewModel.PhotoViewItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTvGalleryRegisterSelected.setOnClickListener {
            fragmentManager?.let { it1 ->
                TagBottomSheetDialog.create(
                    galleryViewModel.selectedPhotos.toMutableList().map {
                        PhotoEntity(
                            id = "${it.path}${galleryViewModel.currentRepo?.id ?: Math.random()}",
                            path = it.path,
                            folderName = it.folderName
                        )
                    },
                    galleryViewModel.currentRepo!!
                ).setOnDismissListener {
                    galleryViewModel.unselectAll()
                    galleryViewModel.init(galleryViewModel.currentPath.value!!)
                }.show(
                    it1, "tag"
                )
            }
        }

        mCbGalSelectAll.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                galleryViewModel.unselectAll()
            }
        }

        mTvGalleryFilter.setOnClickListener {
            galleryViewModel.toggleRegisterFilter()
        }

        galleryViewModel.filterEnabled.observe(context as LifecycleOwner, Observer {

            mTvGalleryFilter.text = if (it) getString(R.string.tagged_only) else getString(R.string.all)
        })

        galleryViewModel.photosFiltered.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it
        })

        galleryViewModel.selectedPhotoSize.observe(context as LifecycleOwner, Observer {
            mTvGalSelectAll.text = "${it} Selected"
            if (it > 0) {
                mTvGalleryRegisterSelected.show()
                mCbGalSelectAll.isChecked = true
            } else {
                mTvGalleryRegisterSelected.hide()
                mCbGalSelectAll.isChecked = false

            }
        })

        galleryViewModel.currentPath.observe(context as LifecycleOwner, Observer {
            mTvGalleryCurrentFolder.text = it
        })

        galleryViewModel.gridType.observe(context as LifecycleOwner, Observer {
            if (it == 1) {
                mTvGalSelectAll.show(false)
                mCbGalSelectAll.show(false)
                mTvGalleryFilter.show(false)
                mTvGalleryFilter.show(false)
            } else {
                mTvGalSelectAll.show(true)
                mCbGalSelectAll.show(true)
                mTvGalleryFilter.show(true)
                mTvGalleryFilter.show(true)
            }
            context?.let { context ->
                adapter = createAdapter(
                    context,
                    if (it == 0) R.layout.item_gallery else R.layout.item_gallery_flat
                )
                mRvGallery.adapter = adapter
                mRvGallery.layoutManager =
                    if (it == 0) GridLayoutManager(context, 3) else LinearLayoutManager(context)
            }
        })
        galleryViewModel.isLoading.observe(context as LifecycleOwner, Observer {
            mPbGallery.show(it)
        })
    }

    private fun createAdapter(
        context: Context,
        id: Int
    ): SimpleRecyclerViewAdapter<GalleryViewModel.PhotoViewItem> {
        return SimpleRecyclerViewAdapter(
            context,
            object :
                SimpleRecyclerViewAdapter.RecyclerProvider<GalleryViewModel.PhotoViewItem>() {
                override fun getLayoutId(): Int {
                    return id
                }

                override fun getItemCount(): Int {
                    return items.size
                }

                override fun getItem(position: Int): GalleryViewModel.PhotoViewItem {
                    return items[position]
                }

                override fun onBindView(
                    containerView: View,
                    item: GalleryViewModel.PhotoViewItem
                ) {
                    if (!item.galleryEntity.isDirectory) {
                        containerView.mTvGalleryFolderName.show(false)
                        Glide.with(context).load(item.galleryEntity.path)
                            .apply(RequestOptions().centerCrop().override(200))
                            .into(containerView.mIvGallery)
                        containerView.mTintGallery.show(item.galleryEntity.isRegistered)
                        containerView.mTvGallerySelected.show(item.isSelected)

                    } else {
                        containerView.mTintGallery.show(false)
                        Glide.with(context).load(item.galleryEntity.thumb)
                            .error(Glide.with(context).load(R.drawable.ic_icons8_folder))
                            .apply(RequestOptions().centerCrop().override(200))
                            .into(containerView.mIvGallery)

                        containerView.mTvGalleryFolderName.show()
                        containerView.mTvGalleryFolderName.text = item.galleryEntity.folderName
                        containerView.mTvGallerySelected.show(false)
                        containerView.mTvGalleryFolderRegisteredCount?.text =
                            "${item.galleryEntity.registeredCount}"
                        containerView.mTvGalleryFolderCount?.text =
                            "/ ${getString(R.string.photo_unit).format(item.galleryEntity.childCount)}"
                    }
                }

                override fun onClick(adapterPosition: Int) {
                    val item = items[adapterPosition]
                    if (item.galleryEntity.isDirectory) {
                        Log.i(this@GalleryFragment.tag(), "onClick ${item.galleryEntity.path}")
                        galleryViewModel.init(item.galleryEntity.path)
                    } else {
                        galleryViewModel.register(item.galleryEntity)
                    }
                }

                override fun setItem(
                    adapter: SimpleRecyclerViewAdapter<GalleryViewModel.PhotoViewItem>,
                    oldItems: List<GalleryViewModel.PhotoViewItem>,
                    newItems: List<GalleryViewModel.PhotoViewItem>
                ) {
                    var removedItems = 0
                    (0 until max(oldItems.size, newItems.size)).forEach { idx ->
                        val old = oldItems.getOrNull(idx)
                        val new = newItems.getOrNull(idx)

                        when {
                            old == null && new == null -> {

                            }
                            old == null && new != null -> {
                                adapter.notifyItemInserted(idx)
                            }
                            old != null && new == null -> {
                                adapter.notifyItemRemoved(idx - removedItems++)
                            }
                            else -> {
                                adapter.notifyItemChanged(idx)
                            }
                        }
                    }


                }
            })
    }


    override fun onResume() {
        super.onResume()
        galleryViewModel.init()
    }

    override fun onBackPress(function: () -> Unit) {
        when {
            galleryViewModel.isLoading.value!! -> {
                return
            }
            galleryViewModel.selectedPhotoSize.value!! != 0 -> {
                mCbGalSelectAll.isChecked = false
                galleryViewModel.unselectAll()
            }
            galleryViewModel.hasBackStack() -> {
                galleryViewModel.goBack()
            }
            else -> {
                function()
            }
        }
    }

}