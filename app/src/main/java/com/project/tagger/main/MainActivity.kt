package com.project.tagger.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.project.tagger.R
import com.project.tagger.gallery.GalleryFragment
import com.project.tagger.my.MyFragment
import com.project.tagger.registeredGallery.RegisteredGalleryFragment
import com.project.tagger.util.Initializer
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var adapter: MainPagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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