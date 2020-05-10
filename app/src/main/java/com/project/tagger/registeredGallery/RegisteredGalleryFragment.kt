package com.project.tagger.registeredGallery

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.project.tagger.R
import com.project.tagger.gallery.PhotoEntity
import com.project.tagger.util.showInvisible
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SelectorAdapter
import com.project.tagger.util.widget.SelectorBottomSheetDialogBuilder
import com.project.tagger.util.widget.SelectorItem
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_registered_gallery.*
import kotlinx.android.synthetic.main.item_registered_gallery.view.*
import org.koin.android.ext.android.inject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random


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
    val tagColors = listOf(
        R.color.tag_color_0,
        R.color.tag_color_1,
        R.color.tag_color_2,
        R.color.tag_color_3
    )

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
                                .apply(
                                    RequestOptions().centerCrop()
                                        .fitCenter()
                                        .placeholder(R.drawable.gallery_placeholder)
                                )
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(containerView.mIvRegGalGallery)

                            containerView.mCgRegGalTags.apply {
                                removeAllViews()
                                item.tags.forEach { tag ->
                                    val tv = TextView(context).apply {
                                        text = "#${tag.tag}"
                                        setTextColor(ContextCompat.getColor(context, R.color.grey))
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

                        containerView.mIvRegGalShare.setOnClickListener {
                            shareImage(item.path)
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
                            if (galleryViewModel.repoAuthState.value == RegisteredGalleryViewModel.RepoUserState.VISITOR) {
                                Toast.makeText(
                                    context,
                                    "Visitors cannot edit photos",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            } else {

//                            shareImage(item.path)

                                RegisteredDetailActivity.create(
                                    context,
                                    item,
                                    galleryViewModel.currentRepo.value!!,
                                    item.tags
                                )
                            }

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
        }
        mRvRegGal.adapter = adapter
        mRvRegGal.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)


        galleryViewModel.repos.observe(this, Observer { repo ->
            mIvRegGalRepo.setOnClickListener {
                SelectorBottomSheetDialogBuilder(requireContext(), layoutInflater)
                    .setTitle("Select a repository")
                    .setItems(repo.map { SelectorItem(it.name) })
                    .setOnItemClickListener(object :
                        SelectorAdapter.OnItemClickListener<SelectorItem> {
                        override fun onClick(
                            dialog: BottomSheetDialog,
                            item: SelectorItem,
                            position: Int
                        ) {
                            galleryViewModel.openRepo(item.content)
                            dialog.dismiss()
                        }

                    })
                    .build()
                    .show()
            }
        })
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
                    galleryViewModel.query(listOf(mEtRegGalSearch.text.toString()))
                },
                onError = {

                })

        mIvRegGalSearch.setOnClickListener {
            galleryViewModel.toggleSearch()
        }

        mRvRegGal.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= mRvRegGal.scrollY) {
                    mAppBarRegGal.elevation = 0f
                } else {
                    mAppBarRegGal.elevation = 8f
                }
            }
        })

        mAppBarRegGal.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->

            mTvRegGalRepoName.alpha =
                1 + (verticalOffset / mAppBarRegGal.totalScrollRange.toFloat())
            mTvRegGalPro.alpha = 1 + (verticalOffset / mAppBarRegGal.totalScrollRange.toFloat())
        })

        observeLiveData()
    }

    private fun observeLiveData() {
        galleryViewModel.isInSearchMode.observe(
            context as LifecycleOwner,
            Observer { isInSearchMode ->
                mEtRegGalSearch.showInvisible(isInSearchMode, 200)
                mHorizontalChipRegGal.showInvisible(!isInSearchMode, 200)

                if (isInSearchMode) {
                    mEtRegGalSearch.requestFocus()
                    Glide.with(this).load(R.drawable.ic_close_black_24dp)
                        .apply(RequestOptions().placeholder(R.drawable.ic_close_black_24dp))
                        .into(mIvRegGalSearch)
                } else {
                    Glide.with(this).load(R.drawable.ic_search_black_24dp)
                        .apply(RequestOptions().placeholder(R.drawable.ic_search_black_24dp))
                        .into(mIvRegGalSearch)

                }
            })
        galleryViewModel.photos.observe(context as LifecycleOwner, Observer {
            Log.i(tag(), "get new photos")
            adapter.provider.items = it

        })

        galleryViewModel.currentRepo.observe(context as LifecycleOwner, Observer {
            mTvRegGalRepoName.text = it.name
        })

        galleryViewModel.repoAuthState.observe(context as LifecycleOwner, Observer { state ->
            when (state) {
                RegisteredGalleryViewModel.RepoUserState.PRO -> {
                    mTvRegGalPro.text = "PRO"
                    mTvRegGalPro.setTextColor(ContextCompat.getColor(context!!, R.color.red))
                }
                RegisteredGalleryViewModel.RepoUserState.BASIC -> {
                    mTvRegGalPro.text = "BASIC"
                    mTvRegGalPro.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                }
                RegisteredGalleryViewModel.RepoUserState.VISITOR -> {
                    mTvRegGalPro.text = "VISITOR"
                    mTvRegGalPro.setTextColor(ContextCompat.getColor(context!!, R.color.orange))
                }
            }
        })

        galleryViewModel.popularTags.observe(context as LifecycleOwner, Observer { tags ->
            mChipGroupRegGal.removeAllViews()
            mChipGroupRegGal.setOnCheckedChangeListener { group, checkedId ->
                val chip = mChipGroupRegGal.findViewById<Chip>(checkedId)
                //TODO using view.tag to pass data
                if (chip != null) {
                    galleryViewModel.query(listOf(chip.tag as String))
                } else {
                    galleryViewModel.query(listOf(""))

                }
            }
            tags.forEach { tag ->
                mChipGroupRegGal.addView(
                    (layoutInflater.inflate(
                        R.layout.chip_filter_layout,
                        mChipGroupRegGal,
                        false
                    ) as Chip)
                        .apply {
                            text = tag.tag
                            this.tag = tag.tag
                            setTextColor(ContextCompat.getColor(context, R.color.white))
                            chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    tagColors[tag.hashCode() % tagColors.size]
                                )
                            )
                        }
                )
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
        startActivityForResult(Intent.createChooser(intent, getString(R.string.share)), 100)
    }


}