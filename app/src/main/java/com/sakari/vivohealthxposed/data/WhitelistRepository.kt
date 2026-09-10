package com.sakari.vivohealthxposed.data

import android.content.Context
import android.content.SharedPreferences
import com.sakari.vivohealthxposed.Constants
import com.sakari.vivohealthxposed.ModuleApp

/**
 * 白名单持久化：本地镜像 + 框架 RemotePreferences。
 *
 * 本地镜像保证 LSPosed 服务未就绪时界面也能用；RemotePreferences 是 Hook 真正读取的地方。
 */
class WhitelistRepository(context: Context) {

    private val localPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)

    fun loadLocal(): Set<String> {
        val saved = localPrefs.getStringSet(Constants.KEY_PACKAGES, null) ?: return emptySet()
        return saved.filter { it.isNotBlank() }.toCollection(LinkedHashSet())
    }

    /**
     * 写入本地镜像，并在框架服务可用时同步到 RemotePreferences。
     *
     * [SharedPreferences.Editor.commit] 是同步的，调用方需要自行切到 IO 线程。
     */
    fun save(packages: Set<String>): Result<Unit> {
        val snapshot = LinkedHashSet(packages)
        localPrefs.edit().putStringSet(Constants.KEY_PACKAGES, snapshot).apply()

        val service = ModuleApp.getService() ?: return Result.success(Unit)
        return runCatching {
            val remote = service.getRemotePreferences(Constants.PREFS_GROUP)
            // 传新集合：避免 SharedPreferences 持有同一个可变引用。
            remote.edit().putStringSet(Constants.KEY_PACKAGES, LinkedHashSet(snapshot)).commit()
        }.map { }
    }

    companion object {
        private const val LOCAL_PREFS = "local"
    }
}
