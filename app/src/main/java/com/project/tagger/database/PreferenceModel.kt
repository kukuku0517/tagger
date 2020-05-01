package com.project.tagger.database

import android.content.Context
import android.content.SharedPreferences


interface PreferenceModel {
    fun <T> putPref(
        key: String,
        value: T
    )

    fun <T> getPref(
        key: String,
        value: T
    ): T

    fun getPref(
        key: String,
        value: Iterable<String>
    ): List<String>

    fun putPref(
        key: String,
        value: Iterable<String>
    )
}

class PreferenceModelImpl(val context: Context) : PreferenceModel {

    private fun getPref(): SharedPreferences? {
        return context.applicationContext?.getSharedPreferences("TAGGER", Context.MODE_PRIVATE)
    }

    override fun <T> putPref(
        key: String,
        value: T
    ) {
        val edit = getPref()?.edit()
        when (value) {
            is Int -> edit?.putInt(key, value)?.apply()
            is String -> edit?.putString(key, value)?.apply()
            is Boolean -> edit?.putBoolean(key, value)?.apply()
            is Float -> edit?.putFloat(key, value)?.apply()
        }
    }

    override fun putPref(
        key: String,
        value: Iterable<String>
    ) {
        val edit = getPref()?.edit()
        edit?.putStringSet(key, value.toMutableSet())?.apply()
    }

    override fun <T> getPref(key: String, value: T): T {
        return when (value) {
            is String -> getPref()?.getString(key, value) as T
            is Boolean -> getPref()?.getBoolean(key, value) as T
            is Float -> getPref()?.getFloat(key, value) as T
            is Int -> getPref()?.getInt(key, value) as T//int
            else -> value
        }
    }

    override fun getPref(key: String, value: Iterable<String>): List<String> {
        return getPref()?.getStringSet(key, value.toSet())?.toList() ?: value.toList()
    }
}
