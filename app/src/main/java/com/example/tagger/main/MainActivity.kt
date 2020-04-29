package com.example.tagger.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.tagger.R
import com.example.tagger.gallery.GalleryFragment
import com.example.tagger.registeredGallery.RegisteredGalleryFragment
import com.example.tagger.util.Initializer
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
    }

    override fun getItem(position: Int): Fragment {

        return when (position) {
            POSITION_TAGGED -> {
                RegisteredGalleryFragment()
            }
            POSITION_GALLERY -> {
                GalleryFragment()
            }
            else -> GalleryFragment()
        }
    }


    override fun getCount(): Int {
        return 2
    }

    fun getItemByTag(position: Int, viewPagerId: Int): Fragment? {
        val f = fm.findFragmentByTag("android:switcher:$viewPagerId:$position")
        return f
    }
}