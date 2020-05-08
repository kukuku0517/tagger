package com.project.tagger.my

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.project.tagger.R
import com.project.tagger.databinding.FragmentMyBinding
import com.project.tagger.login.GetUserUC
import com.project.tagger.login.LoginActivity
import com.project.tagger.login.UserRepository
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myViewModel.init()
        val provider = object : SimpleRecyclerViewAdapter.RecyclerProvider<RepoEntity>() {
            override fun getLayoutId(): Int {
                return R.layout.item_repo
            }

            override fun onBindView(containerView: View, item: RepoEntity) {
                containerView.mTvRepoTitle.text = item.name
                containerView.mTvRepoPremium.text = if (item.isBackUp) "PREMIUM" else "BASIC"
                containerView.mTvRepoPhotos.text = "${item.photos.size}개"
            }

            override fun onClick(adapterPosition: Int) {

            }


        }

        mRvMyRepo.adapter = SimpleRecyclerViewAdapter(requireContext(), provider)
        mRvMyRepo.layoutManager = LinearLayoutManager(requireContext())

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
            }
        })
    }
}

@BindingAdapter("android:src")
fun ImageView.setImage(path: String) {
    Glide.with(this).load(path).into(this)
}