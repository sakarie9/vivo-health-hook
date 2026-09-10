package com.sakari.vivohealthxposed.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakari.vivohealthxposed.Constants
import com.sakari.vivohealthxposed.ModuleApp
import com.sakari.vivohealthxposed.data.AppEntry
import com.sakari.vivohealthxposed.data.AppListLoader
import com.sakari.vivohealthxposed.data.BuiltinWhitelist
import com.sakari.vivohealthxposed.data.LauncherIconController
import com.sakari.vivohealthxposed.data.WhitelistRepository
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 列表筛选方式。 */
enum class AppFilter { ALL, SELECTED, BUILT_IN }

/** 手动输入包名的结果。 */
enum class AddResult { ADDED, DUPLICATE, BUILT_IN }

/** LSPosed 框架连接状态。 */
data class ServiceStatus(
    val connected: Boolean,
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val apiVersion: Int = 0,
)

/**
 * 界面状态与数据来源。
 *
 * - 自定义勾选立即写入本地镜像与框架 RemotePreferences；
 * - 内置白名单来自打包进 res/raw 的静态快照，只读展示；
 * - 已安装应用列表在 IO 线程读取，避免阻塞首帧。
 */
class WhitelistViewModel(application: Application) :
    AndroidViewModel(application), ModuleApp.ServiceStateListener {

    private val repository = WhitelistRepository(application)
    private var mergedRemoteOnce = false

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var builtInPackages by mutableStateOf<Set<String>>(emptySet())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    var loading by mutableStateOf(true)
        private set
    var query by mutableStateOf("")
    var filter by mutableStateOf(AppFilter.ALL)
    var serviceStatus by mutableStateOf(ServiceStatus(connected = false))
        private set
    var remoteError by mutableStateOf<String?>(null)
        private set
    var iconHidden by mutableStateOf(false)
        private set

    init {
        val builtIn = BuiltinWhitelist.load(application)
        builtInPackages = builtIn
        // 内置项始终生效，不应再存进用户自定义集合（兼容旧数据里的重复项）。
        selected = repository.loadLocal() - builtIn
        serviceStatus = ModuleApp.getService().toStatus()
        iconHidden = LauncherIconController.isHidden(application)
        ModuleApp.addListener(this)
        refreshApps()
    }

    fun refreshApps() {
        loading = true
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { AppListLoader.load(getApplication()) }
            apps = loaded
            loading = false
        }
    }

    /** 内置项无需勾选，直接忽略。 */
    fun toggle(packageName: String) {
        if (packageName in builtInPackages) return
        selected = if (packageName in selected) selected - packageName else selected + packageName
        persist()
    }

    /** 手动补充一个包名。 */
    fun addManual(packageName: String): AddResult {
        if (packageName in builtInPackages) return AddResult.BUILT_IN
        if (packageName in selected) return AddResult.DUPLICATE
        selected = selected + packageName
        persist()
        return AddResult.ADDED
    }

    fun clearAll() {
        if (selected.isEmpty()) return
        selected = emptySet()
        persist()
    }

    fun consumeRemoteError() {
        remoteError = null
    }

    /** 隐藏 / 恢复桌面图标（切换 manifest 里的 LauncherAlias）。 */
    fun applyIconHidden(hidden: Boolean) {
        LauncherIconController.setHidden(getApplication(), hidden)
        iconHidden = hidden
    }

    override fun onServiceChanged(service: XposedService?) {
        serviceStatus = service.toStatus()
        if (service != null && !mergedRemoteOnce) {
            mergedRemoteOnce = true
            mergeRemote(service)
        }
    }

    /** 首次连上框架时把远程配置并入本地，避免两边不一致；内置项不参与。 */
    private fun mergeRemote(service: XposedService) {
        viewModelScope.launch {
            val remote = withContext(Dispatchers.IO) {
                runCatching {
                    service.getRemotePreferences(Constants.PREFS_GROUP)
                        .getStringSet(Constants.KEY_PACKAGES, null)
                        ?.filter { it.isNotBlank() }
                        ?.toSet()
                        .orEmpty()
                }.getOrDefault(emptySet())
            }
            val merged = (selected + remote) - builtInPackages
            if (merged != selected) {
                selected = merged
            }
            persist()
        }
    }

    private fun persist() {
        val snapshot = selected
        viewModelScope.launch {
            repository.save(snapshot).onFailure {
                remoteError = it.message ?: it.toString()
            }
        }
    }

    override fun onCleared() {
        ModuleApp.removeListener(this)
        super.onCleared()
    }
}

private fun XposedService?.toStatus(): ServiceStatus = if (this == null) {
    ServiceStatus(connected = false)
} else {
    ServiceStatus(true, frameworkName, frameworkVersion, apiVersion)
}
