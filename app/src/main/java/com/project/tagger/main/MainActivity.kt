package com.project.tagger.main

import android.R.attr.button
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.project.tagger.BuildConfig
import com.project.tagger.R
import com.project.tagger.gallery.GalleryFragment
import com.project.tagger.my.MyFragment
import com.project.tagger.registeredGallery.RegisteredGalleryFragment
import com.project.tagger.util.EventKey
import com.project.tagger.util.FA
import com.project.tagger.util.Initializer
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var adapter: MainPagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mAdView = AdView(this)
        mAdView.adSize = AdSize.BANNER
        val adRequest = AdRequest.Builder().build()
        if (BuildConfig.DEBUG) {
            mAdView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        } else {
            mAdView.adUnitId = "ca-app-pub-9887170196921581/2335218521"
        }
        mLayoutAdView.addView(mAdView)
        mAdView.loadAd(adRequest)

        Initializer(this).getPermissions(this, *Initializer.permissions)
            .subscribeBy(onNext = {
                adapter = MainPagerAdapter(supportFragmentManager)
                mVpMain.adapter = adapter
                mVpMain.offscreenPageLimit = 3
                mVpMain.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(mTlMain))
                mTlMain.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        if (tab != null) {
                            mVpMain.currentItem = tab.position
                            when (tab.position) {
                                0 -> FA.logData(EventKey.TAB_REPO)
                                1 -> FA.logData(EventKey.TAB_GALLERY)
                                2 -> FA.logData(EventKey.TAB_MY)
                            }
                        }
                    }

                })
            }, onError = {})


    }


    override fun onBackPressed() {
        val currentFragment = adapter.getItemByTag(mVpMain.currentItem, mVpMain.id)
        if (currentFragment is MainPagerAdapter.FragmentBackPressListener) {
            currentFragment.onBackPress {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }

    }
}

class MainPagerAdapter(val fm: androidx.fragment.app.FragmentManager) :
    androidx.fragment.app.FragmentPagerAdapter(fm) {

    interface FragmentBackPressListener {
        fun onBackPress(function: () -> Unit)
    }

    companion object {
        val POSITION_TAGGED = 0
        val POSITION_GALLERY = 1
        val POSITION_MY = 2
    }

    override fun getItem(position: Int): Fragment {

        return when (position) {
            POSITION_TAGGED -> {
                RegisteredGalleryFragment()
            }
            POSITION_GALLERY -> {
                GalleryFragment()
            }
            POSITION_MY -> {
                MyFragment()
            }
            else -> GalleryFragment()
        }
    }


    override fun getCount(): Int {
        return 3
    }

    fun getItemByTag(position: Int, viewPagerId: Int): Fragment? {
        val f = fm.findFragmentByTag("android:switcher:$viewPagerId:$position")
        return f
    }
}