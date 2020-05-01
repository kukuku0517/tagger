package com.project.tagger.registeredGallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_registered_gallery.*
import kotlinx.android.synthetic.main.item_registered_gallery.view.*
import org.koin.android.ext.android.inject
import java.io.File

class RegisteredGalleryFragment : Fragment() {
    val galleryViewModel: RegisteredGalleryViewModel by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(tag(), "onCreateView")
        return inflater.inflate(R.layout.fragment_registered_gallery, container, false)
    }

    lateinit var adapter: SimpleRecyclerViewAdapter<PhotoEntity>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { context ->
            adapter = SimpleRecyclerViewAdapter(
                context,
                object : SimpleRecyclerViewAdapter.RecyclerProvider<PhotoEntity>() {
                    override fun getLayoutId(): Int {
                        return R.layout.item_registered_gallery
                    }

                    override fun getItemCount(): Int {
                        return items.size
                    }

                    override fun getItem(position: Int): PhotoEntity {
                        return items[position]
                    }

                    override fun onBindView(containerView: View, item: PhotoEntity) {
                        if (!item.isDirectory) {
                            Glide.with(context).load(item.path).apply(RequestOptions().centerCrop())
                                .into(containerView.mIvRegGalGallery)
                            containerView.mCgRegGalTags.apply {
                                removeAllViews()
                                item.tags.forEach {
                                    addView(Chip(context).apply { text = it.tag })
                                }
                            }

                        } else {
                            containerView.mIvRegGalGallery.setImageDrawable(null)
                        }
                    }

                    override fun onClick(adapterPosition: Int) {
                        val item = items[adapterPosition]
                        if (item.isDirectory) {
                            Log.i(this@RegisteredGalleryFragment.tag(), "onClick ${item.path}")

                        } else {
                            Log.i(
                                this@RegisteredGalleryFragment.tag(),
                                "${item.tags.map { it.tag }.reduce { acc, tag -> "$acc $tag" }}"
                            )
                            shareImage(item.path)

                        }
                    }
                })
            mRvRegGal.adapter = adapter
            mRvRegGal.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }

        mEtRegGalSearch.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    galleryViewModel.query(mEtRegGalSearch.text.toString())
                }
            }

            true
        }

        galleryViewModel.photos.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it
            adapter.notifyDataSetChanged()
        })

        galleryViewModel.currentRepo.observe(context  as LifecycleOwner, Observer {
            mTvRegGalRepoName.text = it.name
        })

    }

    override fun onResume() {
        super.onResume()
        galleryViewModel.init()
    }

    fun shareImage(path: String) {
        val type = "image/*";
        val imageFile = File(path)


        val intent = Intent(Intent.ACTION_SEND)
        intent.type = type

        val uri: Uri
        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                imageFile
            )
        } else {
            Uri.fromFile(imageFile)
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivityForResult(Intent.createChooser(intent, "공유"), 100)
    }


}