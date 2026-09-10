package com.sakari.vivohealthxposed.data

private val PACKAGE_NAME_PATTERN =
    Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

/** 校验是否是合法的 Android 包名，例如 com.netease.cloudmusic。 */
fun isValidPackageName(value: String): Boolean = PACKAGE_NAME_PATTERN.matches(value)
