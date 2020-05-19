package com.project.tagger.my

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.project.tagger.GlobalApp
import com.project.tagger.R
import com.project.tagger.databinding.FragmentMyBinding
import com.project.tagger.login.LoginActivity
import com.project.tagger.premium.PurchaseActivity
import com.project.tagger.repo.RepoEntity
import com.project.tagger.util.widget.SimpleRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_my.*
import kotlinx.android.synthetic.main.item_repo.view.*
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

    fun checkForPremium(repoEntity: RepoEntity) {
        startActivity(Intent(context, PurchaseActivity::class.java).apply {
            putExtra(PurchaseActivity.REPO, repoEntity)
        })
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
            provider.items = it
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
    }
}

@BindingAdapter("android:src")
fun ImageView.setImage(path: String) {
    Glide.with(this).load(path).apply(RequestOptions().circleCrop()).into(this)
}