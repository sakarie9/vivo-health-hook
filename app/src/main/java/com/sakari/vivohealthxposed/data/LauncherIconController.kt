package com.sakari.vivohealthxposed.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 通过启用 / 禁用 manifest 里的 `LauncherAlias` 来隐藏或恢复桌面图标。
 *
 * 真正的 `MainActivity` 保持 exported 且不带 LAUNCHER，所以隐藏图标后仍可用显式组件启动
 * （adb 或 LSPosed 管理器），不会出现「隐藏后再也打不开」的问题。
 */
object LauncherIconController {

    private const val ALIAS_CLASS = "com.sakari.vivohealthxposed.LauncherAlias"

    fun isHidden(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(alias(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun setHidden(context: Context, hidden: Boolean) {
        val state = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            alias(context),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun alias(context: Context): ComponentName =
        ComponentName(context.packageName, ALIAS_CLASS)
}
