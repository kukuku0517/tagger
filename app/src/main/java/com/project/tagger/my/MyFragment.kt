package com.project.tagger.my

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import com.project.tagger.GlobalApp
import com.project.tagger.R
import com.project.tagger.databinding.FragmentMyBinding
import com.project.tagger.login.LoginActivity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.tag
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_my.*
import kotlinx.android.synthetic.main.item_repo.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class MyFragment : Fragment() {

    val myViewModel: MyViewModel by inject()

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

    lateinit private var billingClient: BillingClient
    fun checkForPremium() {
        context?.let { context ->

            billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(object : PurchasesUpdatedListener {
                    override fun onPurchasesUpdated(
                        p0: BillingResult?,
                        p1: MutableList<Purchase>?
                    ) {

                    }
                }).build()
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                    }
                    GlobalScope.launch {
                        val details = querySkuDetails()
                        Log.i(this@MyFragment.tag(), "$details")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }

    }

    suspend fun querySkuDetails(): SkuDetailsResult {
        val skuList = ArrayList<String>()
        skuList.add("premium_upgrade")
        skuList.add("gas")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
        return skuDetailsResult
        // Process the result.
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myViewModel.init()
        val provider = object : SimpleRecyclerViewAdapter.RecyclerProvider<RepoEntity>() {
            override fun getLayoutId(): Int {
                return R.layout.item_repo
            }

            override fun onBindView(containerView: View, item: RepoEntity) {
                containerView.mTvRepoTitle.text = item.name
                containerView.mTvRepoPremium.text = if (item.backUp) "PREMIUM" else "BASIC"
                containerView.mTvRepoPhotos.text = "${item.photos.size}개"
            }

            override fun onClick(adapterPosition: Int) {
//                checkForPremium()

                items[adapterPosition].apply {
                    if (!this.backUp) {
                        myViewModel.setPremium(this)
                    }
                }

            }


        }

        mRvMyRepo.adapter = SimpleRecyclerViewAdapter(requireContext(), provider)
        mRvMyRepo.layoutManager = LinearLayoutManager(requireContext())

        mEtMyRepoId.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    myViewModel.addRepo(mEtMyRepoId.text.toString().toInt())
                    mEtMyRepoId.text = null
                }
            }

            true
        }
        myViewModel.repos.observe(context as LifecycleOwner, Observer {
            provider.items = it
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
    }
}

@BindingAdapter("android:src")
fun ImageView.setImage(path: String) {
    Glide.with(this).load(path).into(this)
}