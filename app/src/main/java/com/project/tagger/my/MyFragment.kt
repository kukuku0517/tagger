package com.project.tagger.my

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.project.tagger.BuildConfig
import com.project.tagger.GlobalApp
import com.project.tagger.R
import com.project.tagger.databinding.FragmentMyBinding
import com.project.tagger.login.LoginActivity
import com.project.tagger.premium.PurchaseActivity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import com.project.tagger.util.widget.TransparentDialog
import kotlinx.android.synthetic.main.fragment_my.*
import kotlinx.android.synthetic.main.item_repo.view.*
import org.koin.android.ext.android.inject


class MyFragment : Fragment(), RewardedVideoAdListener {

    val myViewModel: MyViewModel by inject()

    override fun onPause() {
        mRewardedVideoAd.pause(context)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        mRewardedVideoAd.resume(context)
    }

    override fun onDestroy() {
        mRewardedVideoAd.destroy(context)
        super.onDestroy()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMyBinding.inflate(inflater, container, false)
        binding.vm = myViewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    fun checkForPremium(repoEntity: RepoEntity) {
        startActivity(Intent(context, PurchaseActivity::class.java).apply {
            putExtra(PurchaseActivity.REPO, repoEntity)
        })
    }

    private lateinit var mRewardedVideoAd: RewardedVideoAd
    var pendingRepo: RepoEntity? = null
    var errorCount = 0

    private fun loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(
            if (BuildConfig.DEBUG)
                "ca-app-pub-3940256099942544/5224354917"
            else
                "ca-app-pub-9887170196921581/9278112381",
            AdRequest.Builder().build()
        )
    }

    private fun showRewardedVideoAd(repoEntity: RepoEntity) {
        pendingRepo = repoEntity
        if (mRewardedVideoAd.isLoaded) {
            mRewardedVideoAd.show()
        } else {
            Toast.makeText(context, getString(R.string.ad_loading_another), Toast.LENGTH_SHORT)
                .show()
            loadRewardedVideoAd()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myViewModel.init()
        // Use an activity context to get the rewarded video instance.
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(context)
        mRewardedVideoAd.rewardedVideoAdListener = this

        loadRewardedVideoAd()

        val provider = object : SimpleRecyclerViewAdapter.RecyclerProvider<RepoEntity>() {
            override fun getLayoutId(): Int {
                return R.layout.item_repo
            }

            override fun onBindView(containerView: View, item: RepoEntity) {
                Glide.with(requireContext()).load(R.drawable.ic_content_copy_black_24dp)
                    .into(containerView.mIvRepoItemShare)
                containerView.mTvRepoTitle.text = item.name
                containerView.mTvRepoPremium.text =
                    if (item.backUp) getString(R.string.premium) else getString(
                        R.string.basic
                    )
                containerView.mTvRepoPhotos.text =
                    getString(R.string.photo_unit).format(item.photos.size)
                Glide.with(requireContext()).load(item.thumb).into(containerView.mIvRepoItemThumb)
                containerView.mIvRepoItemShare.setOnClickListener {

                    val clipboardManager: ClipboardManager =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Repo id", item.id.toString())
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(requireContext(), "ID copied : ${item.id}", Toast.LENGTH_LONG)
                        .show()
                }

                Glide.with(requireContext()).load(
                    if (myViewModel.isMyRepo(item)) {
                        R.drawable.ic_file_upload_black_24dp
                    } else {
                        R.drawable.ic_file_download_black_24dp
                    }
                ).into(
                    containerView.mIvRepoItemSync
                )
                containerView.mIvRepoItemSync.setOnClickListener {
                    //                    if ) {
//                        myViewModel.syncRepository(item)
//                    } else {
                    showRewardedVideoAd(item)
//                    }
                }

            }

            override fun onClick(adapterPosition: Int) {
                if (!items[adapterPosition].backUp) {
                    checkForPremium(items[adapterPosition])
                }


//                items[adapterPosition].apply {
//                    if (!this.backUp) {
//                        myViewModel.setPremium(this)
//                    }
//                }

            }


        }

        mRvMyRepo.adapter = SimpleRecyclerViewAdapter(requireContext(), provider)
        mRvMyRepo.layoutManager = LinearLayoutManager(requireContext())

        mBtnMyAddRepo.setOnClickListener {
            fragmentManager?.let { it1 -> RepoSearchDialogFragment().show(it1, "RepoSearch") }
        }
        myViewModel.repos.observe(context as LifecycleOwner, Observer {
            provider.items = it.sortedByDescending { myViewModel.isMyRepo(it) }
        })

        myViewModel.isLoading.observe(this, Observer {
            if (it) {
                TransparentDialog.showLoading(context!!)
            } else {
                TransparentDialog.hideLoading()
            }
        })


        myViewModel.signOutDialogEvent.observe(context as LifecycleOwner, Observer {
            if (it) {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.sign_out_small))
                    .setMessage(getString(R.string.sign_out_confirm_message))
                    .setPositiveButton(getString(R.string.sign_out_small)) { d, _ ->
                        myViewModel.signOutConfirmed()
                        d.dismiss()
                    }
                    .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                        d.dismiss()
                    }
                    .show()
            }
        })

        myViewModel.signOutEvent.observe(context as LifecycleOwner, Observer {
            if (it) {
                startActivity(Intent(context, LoginActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                activity?.finish()
                context?.let {
                    (it.applicationContext as GlobalApp).restartKoin()
                }
            }
        })
        myViewModel.toastEvent.observe(context as LifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onRewardedVideoAdClosed() {
        Log.i(tag(), "onRewardedVideoAdClosed")
    }

    override fun onRewardedVideoAdLeftApplication() {
    }

    override fun onRewardedVideoAdLoaded() {
    }

    override fun onRewardedVideoAdOpened() {
    }

    override fun onRewardedVideoCompleted() {
        Log.i(tag(), "onRewardedVideoCompleted")

    }

    override fun onRewarded(p0: RewardItem?) {
        Log.i(tag(), "onRewarded")
        if (pendingRepo != null) {
            myViewModel.syncRepository(pendingRepo!!)
            pendingRepo = null
            errorCount = 0
        } else {
            errorCount++
            Toast.makeText(
                context,
                "Sorry, unexpected error occurred. Please try again",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRewardedVideoStarted() {
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
        Log.i(tag(), "onRewardedVideoAdFailedToLoad ${p0}")
        if (pendingRepo != null && errorCount > 3) {
            errorCount = 0
            myViewModel.syncRepository(pendingRepo!!)
            pendingRepo = null
        } else {
            errorCount++
            Toast.makeText(
                context,
                getString(R.string.ad_sorry),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@BindingAdapter("android:src")
fun ImageView.setImage(path: String) {
    Glide.with(this).load(path).apply(RequestOptions().circleCrop()).into(this)
}