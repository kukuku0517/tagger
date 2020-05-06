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
import kotlinx.android.synthetic.main.item_gallery.view.*
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
        mTvGalSelectAll.setOnClickListener {
            galleryViewModel.selectAll()
        }

        mTvGalUnSelectAll.setOnClickListener {
            galleryViewModel.unselectAll()
        }

        galleryViewModel.photos.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it
        })

        galleryViewModel.hasSelectedPhotos.observe(context as LifecycleOwner, Observer {
            if (it) {
                mTvGalleryRegisterSelected.show()
            } else {
                mTvGalleryRegisterSelected.hide()

            }
        })

        galleryViewModel.currentPath.observe(context as LifecycleOwner, Observer {
            mTvGalleryCurrentFolder.text = it
        })

        galleryViewModel.gridType.observe(context as LifecycleOwner, Observer {
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
                    if (!item.photoEntity.isDirectory) {
                        containerView.mTvGalleryFolderName.show(false)
                        Glide.with(context).load(item.photoEntity.path)
                            .apply(RequestOptions().centerCrop().override(200))
                            .into(containerView.mIvGallery)
                        containerView.mTintGallery.show(item.photoEntity.isRegistered)
                        containerView.mTvGallerySelected.show(item.isSelected)

                    } else {
                        containerView.mTintGallery.show(false)
                        Glide.with(context).load(item.photoEntity.thumb)
                            .error(Glide.with(context).load(R.drawable.ic_icons8_folder))
                            .apply(RequestOptions())
                            .into(containerView.mIvGallery)

                        containerView.mTvGalleryFolderName.show()
                        containerView.mTvGalleryFolderName.text = item.photoEntity.folderName
                        containerView.mTvGallerySelected.show(false)
                        containerView.mTvGalleryFolderCount?.text =
                            item.photoEntity.childCount.toString()
                    }
                }

                override fun onClick(adapterPosition: Int) {
                    val item = items[adapterPosition]
                    if (item.photoEntity.isDirectory) {
                        Log.i(this@GalleryFragment.tag(), "onClick ${item.photoEntity.path}")
                        galleryViewModel.init(item.photoEntity.path)
                    } else {
                        galleryViewModel.register(item.photoEntity)
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
            galleryViewModel.hasSelectedPhotos.value!! -> {
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