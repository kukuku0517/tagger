package com.project.tagger.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.project.tagger.R
import com.project.tagger.util.show
import kotlinx.android.synthetic.main.dialog_repo_search.*
import kotlinx.android.synthetic.main.item_repo.view.*
import org.koin.android.ext.android.inject

class RepoSearchDialogFragment : DialogFragment() {
    val repoSearchViewModel: RepoSearchViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(
            STYLE_NO_TITLE,
            R.style.AppTheme_Dialog
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_repo_search, container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repoSearchViewModel.init()

        mEtSearchRepoId.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    repoSearchViewModel.searchRepo(mEtSearchRepoId.text.toString().toInt())
                }
            }

            true
        }

        repoSearchViewModel.repoResult.observe(this, Observer { repo ->
            if (repo != null) {
                mLayoutRepoItem.show(true)
                mLayoutRepoItem.mIvRepoItemShare.show(false)
                mLayoutRepoItem.mTvRepoTitle.text = repo.name
                mLayoutRepoItem.mTvRepoPremium.text =
                    if (repo.backUp) getString(R.string.premium) else getString(R.string.basic)
                mLayoutRepoItem.mTvRepoPhotos.text =
                    getString(R.string.photo_unit).format(repo.photos.size)
            } else {

                mLayoutRepoItem.show(false)
            }
        })

        repoSearchViewModel.repoSearchResultState.observe(this, Observer {
            when (it) {
                RepoSearchViewModel.RepoSearchState.OK -> {
                    mTvSearchRepoConfirm.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.green
                        )
                    )
                    mTvSearchRepoConfirm.text = getString(R.string.add_to_repository)
                }
                RepoSearchViewModel.RepoSearchState.DUPLICATE -> {
                    mTvSearchRepoConfirm.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                    mTvSearchRepoConfirm.text = getString(R.string.repo_search_duplicate)
                }
                RepoSearchViewModel.RepoSearchState.EMPTY -> {
                    mTvSearchRepoConfirm.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.grey
                        )
                    )
                    mTvSearchRepoConfirm.text = getString(R.string.repo_search_empty)
                }
                RepoSearchViewModel.RepoSearchState.ERROR -> {
                    mTvSearchRepoConfirm.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                    mTvSearchRepoConfirm.text = getString(R.string.repo_search_error)
                }
            }
        })

        repoSearchViewModel.repoAddCompleteEvent.observe(this, Observer {
            if(it){
                dismiss()
            }
        })
        mTvSearchRepoConfirm.setOnClickListener {
            repoSearchViewModel.addRepo()
        }
    }
}