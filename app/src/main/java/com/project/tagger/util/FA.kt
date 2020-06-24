package com.project.tagger.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.project.tagger.GlobalApp

class FA {
    companion object {


        fun logData(key: String, bundle: Bundle? = null) {
            GlobalApp.globalContext?.let { context ->
                FirebaseAnalytics.getInstance(context).logEvent(key, bundle)
            }
        }


        fun logData(key: String, map: Map<String, String?>) {
            GlobalApp.globalContext?.let { context ->
                val bundle = Bundle()
                map.forEach {
                    bundle.putString(it.key, it.value)
                }
                FirebaseAnalytics.getInstance(context).logEvent(key, bundle)
            }
        }
    }
}

class EventKey {
    companion object {

        const val SCREEN_LOGIN = "SCREEN_LOGIN"


        const val login_signupgoogle = "login_signupgoogle"
        const val login_signupgoogle_cancel = "login_signupgoogle_cancel"
        const val login_signupgoogle_complete = "login_signupgoogle_complete"

        const val TAB_GALLERY = "TAB_GALLERY"
        const val TAB_REPO = "TAB_REPO"
        const val TAB_MY = "TAB_MY"

        const val repo_item_click = "repo_item_click"
        const val repo_item_share = "repo_item_share"
        const val repo_tag_search = "repo_tag_search"
        const val repo_tag_click = "repo_tag_click"
        const val repo_repo_select = "repo_repo_select"

        const val regdetail_delete = "regdetail_delete"
        const val regdetail_share = "regdetail_share"
        const val regdetail_add_tag = "regdetail_add_tag"
        const val regdetail_complete = "regdetail_complete"
        const val regdetail_add_tag_done = "regdetail_add_tag_done"

        const val gallery_item_click = "gallery_item_click"
        const val gallery_new = "gallery_new"
        const val gallery_toggle_tagged_option = "gallery_toggle_tagged_option"

        const val tagsheet_start = "tagsheet_start"
        const val tagsheet_complete = "tagsheet_complete"
        const val tagsheet_cancel = "tagsheet_cancel"

        const val mypage_upload = "mypage_upload"
        const val mypage_click_for_premium = "mypage_click_for_premium"

        const val mypage_copy = "mypage_copy"
        const val mypage_find_repo = "mypage_find_repo"
        const val mypage_add_repo = "mypage_add_repo"
        const val mypage_logout = "mypage_logout"


    }
}