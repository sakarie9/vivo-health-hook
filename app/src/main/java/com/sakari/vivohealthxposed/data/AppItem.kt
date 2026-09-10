package com.sakari.vivohealthxposed.data

import android.graphics.drawable.Drawable

/**
 * 列表里的一行，统一表示三种来源：
 * - 已安装应用（[installed] = true，有名称与图标）；
 * - vivo 健康内置白名单（[builtIn] = true，始终生效，不可取消）；
 * - 用户手动输入的包名（[selected] = true 且 [installed] = false）。
 */
data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val installed: Boolean,
    val builtIn: Boolean,
    val selected: Boolean,
)

/**
 * 合并已安装应用、内置包名与用户自定义包名。
 *
 * 已安装应用保持传入顺序（调用方已按名称排序）；未安装的内置项与手动项按包名排序后
 * 追加在后面，并自动去重（同一包名只出现一次）。
 */
fun buildAppItems(
    installedApps: List<AppEntry>,
    builtInPackages: Set<String>,
    selectedPackages: Set<String>,
): List<AppItem> {
    val seen = HashSet<String>(installedApps.size + builtInPackages.size + selectedPackages.size)
    val items = ArrayList<AppItem>(installedApps.size + builtInPackages.size)

    installedApps.forEach { app ->
        if (seen.add(app.packageName)) {
            items += AppItem(
                packageName = app.packageName,
                label = app.label,
                icon = app.icon,
                installed = true,
                builtIn = app.packageName in builtInPackages,
                selected = app.packageName in selectedPackages,
            )
        }
    }
    builtInPackages.sorted().forEach { packageName ->
        if (seen.add(packageName)) {
            items += AppItem(
                packageName = packageName,
                label = packageName,
                icon = null,
                installed = false,
                builtIn = true,
                selected = false,
            )
        }
    }
    selectedPackages.sorted().forEach { packageName ->
        if (seen.add(packageName)) {
            items += AppItem(
                packageName = packageName,
                label = packageName,
                icon = null,
                installed = false,
                builtIn = false,
                selected = true,
            )
        }
    }
    return items
}
