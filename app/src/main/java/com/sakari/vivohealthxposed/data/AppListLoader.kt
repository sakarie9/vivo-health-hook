package com.sakari.vivohealthxposed.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** 从 PackageManager 读取带启动图标的应用列表。 */
object AppListLoader {

    /**
     * 返回所有带桌面启动图标的应用（已排除本模块自身），按名称忽略大小写排序。
     *
     * Android 11+ 的包可见性由清单里的 <queries> 声明保证，无需 QUERY_ALL_PACKAGES。
     */
    @Suppress("DEPRECATION")
    fun load(context: Context): List<AppEntry> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(launcherIntent, 0)
        val selfPackage = context.packageName
        val seen = HashSet<String>(resolved.size)

        return resolved.asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != selfPackage && seen.add(it.packageName) }
            .map { it.toEntry(packageManager) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    private fun ApplicationInfo.toEntry(packageManager: PackageManager): AppEntry = AppEntry(
        packageName = packageName,
        label = runCatching { loadLabel(packageManager).toString() }.getOrDefault(packageName),
        icon = runCatching { loadIcon(packageManager) }.getOrNull(),
    )
}
