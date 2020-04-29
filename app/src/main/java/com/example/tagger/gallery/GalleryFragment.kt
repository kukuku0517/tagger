package com.example.tagger.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tagger.R
import com.example.tagger.main.MainPagerAdapter
import com.example.tagger.tag.TagBottomSheetDialog
import com.example.tagger.util.show
import com.example.tagger.util.tag
import com.example.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.android.synthetic.main.item_gallery.view.*
import org.koin.android.ext.android.inject

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

        context?.let { context ->
            adapter = SimpleRecyclerViewAdapter(
                context,
                object :
                    SimpleRecyclerViewAdapter.RecyclerProvider<GalleryViewModel.PhotoViewItem>() {
                    override fun getLayoutId(): Int {
                        return R.layout.item_gallery
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
                                .into(containerView.mIvGallery)
                            containerView.mTvGallerySelected.show(item.isSelected)
                        } else {
                            containerView.mIvGallery.setImageDrawable(null)
                            containerView.mTvGalleryFolderName.show()
                            containerView.mTvGalleryFolderName.text = item.photoEntity.folderName

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
                })
            mRvGallery.adapter = adapter
            mRvGallery.layoutManager = GridLayoutManager(context, 3)
        }

        mTvGalleryRegisterSelected.setOnClickListener {
            fragmentManager?.let { it1 ->
                TagBottomSheetDialog.create(galleryViewModel.selectedPhotos.toMutableList()).show(
                    it1, "tag")
            }
        }

        galleryViewModel.photos.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it
            adapter.notifyDataSetChanged()
        })

        galleryViewModel.hasSelectedPhotos.observe(context as LifecycleOwner, Observer {
            mTvGalleryRegisterSelected.show(it)
        })

    }


    override fun onResume() {
        super.onResume()
        galleryViewModel.init()
    }

    override fun onBackPress(function: () -> Unit) {
        if (galleryViewModel.hasBackStack()) {
            galleryViewModel.goBack()
        } else {
            function()
        }
    }

}