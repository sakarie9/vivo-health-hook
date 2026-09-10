package com.sakari.vivohealthxposed.data

import android.content.Context
import com.sakari.vivohealthxposed.R

/**
 * vivo 健康内置音乐白名单（静态快照）。
 *
 * 数据由 `tools/extract_builtin_whitelist.sh` 从 `MusicCtrlManager.<clinit>`
 * 里的 const-string 抽取，生成到 `res/raw/builtin_whitelist.txt`。
 * 这些包名 Hook 侧本来就会返回，用户无需（也无法）取消，仅供界面展示。
 */
object BuiltinWhitelist {

    fun load(context: Context): Set<String> = runCatching {
        context.resources.openRawResource(R.raw.builtin_whitelist).use { input ->
            input.bufferedReader().useLines { lines ->
                lines.map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toCollection(LinkedHashSet())
            }
        }
    }.getOrDefault(emptySet())
}
