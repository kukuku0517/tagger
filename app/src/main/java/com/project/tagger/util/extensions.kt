package com.project.tagger.util

import android.view.View
import java.util.*

fun Any.tag(): String {
    return this.javaClass.simpleName
}

fun View.show(show: Boolean = true) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}

fun <T> Stack<T>.peekIfNotEmpty(): T? {
    return if (isNotEmpty()){
        peek()
    }else{
        null
    }
}