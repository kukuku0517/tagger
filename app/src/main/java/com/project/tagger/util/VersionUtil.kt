package com.project.tagger.util

import com.project.tagger.login.SplashActivity
import kotlin.math.pow

class VersionUtil {
    companion object {
        fun compare(thiz: String, other: String): Int {
            val latestVersionToken = other.split(".")
            val currentVersionToken = thiz.split(".")

            if (latestVersionToken.size != 3 || currentVersionToken.size != 3) return 0

            for (i in 0..2) {
                if (latestVersionToken[i] != currentVersionToken[i]) {
                    return latestVersionToken[i].toInt() - currentVersionToken[i].toInt()
                }
            }

            if (latestVersionToken[2] != currentVersionToken[2]) {
                val latestInNumber =
                    latestVersionToken[2].toInt() * 10.0.pow((2 - latestVersionToken[2].length).toDouble())
                val currentInNumber =
                    currentVersionToken[2].toInt() * 10.0.pow((2 - currentVersionToken[2].length).toDouble())
                return (latestInNumber - currentInNumber).toInt()
            }

            return 0
        }
    }
}