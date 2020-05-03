package com.project.tagger.registeredGallery

import android.R.attr.radius
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.google.android.material.chip.Chip
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_registered_gallery.*
import kotlinx.android.synthetic.main.item_registered_gallery.view.*
import org.koin.android.ext.android.inject
import java.io.File
import java.util.concurrent.TimeUnit


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

                            Glide
                                .with(context)
                                .load(item.path)
                                .error(Glide.with(context).load(item.remotePath))
                                .apply(RequestOptions().centerCrop())
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(containerView.mIvRegGalGallery)

                            containerView.mCgRegGalTags.apply {
                                removeAllViews()
                                item.tags.forEach { tag ->
                                        val tv = TextView(context).apply {
                                            text = "#${tag.tag}"
                                            setTextColor(ContextCompat.getColor(context,R.color.grey))
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                            )
                                        }
                                        addView(tv)

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

                    override fun setItem(
                        adapter: SimpleRecyclerViewAdapter<PhotoEntity>,
                        oldItems: List<PhotoEntity>,
                        newItems: List<PhotoEntity>
                    ) {
                        adapter.notifyDataSetChanged()
                    }
                })
            mRvRegGal.adapter = adapter
            mRvRegGal.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }

        Observable.create<String> { emitter ->
            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    emitter.onNext(s.toString())
                }
            }

            mEtRegGalSearch.addTextChangedListener(watcher)
            emitter.setCancellable {
                mEtRegGalSearch.removeTextChangedListener(watcher)
            }
        }.debounce(500, TimeUnit.MILLISECONDS)
            .subscribeBy(
                onNext = {
                    galleryViewModel.query(mEtRegGalSearch.text.toString())
                },
                onError = {

                })


        galleryViewModel.photos.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it

        })

        galleryViewModel.currentRepo.observe(context as LifecycleOwner, Observer {
            mTvRegGalRepoName.text = it.name
        })

        galleryViewModel.isBackUp.observe(context as LifecycleOwner, Observer {isBackUp ->
            if (isBackUp){
                mTvRegGalPro.text = "PRO"
                mTvRegGalPro.setTextColor(ContextCompat.getColor(context!!, R.color.red))
            }else{
                mTvRegGalPro.text = "BASIC"
                mTvRegGalPro.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
            }
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