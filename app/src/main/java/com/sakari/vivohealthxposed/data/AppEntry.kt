package com.sakari.vivohealthxposed.data

import android.graphics.drawable.Drawable

/** 一个已安装、可供勾选的应用。 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)
