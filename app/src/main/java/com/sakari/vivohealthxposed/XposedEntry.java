package com.sakari.vivohealthxposed;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * libxposed 现代 API（API 102）的模块入口。
 *
 * <p>入口类由 META-INF/xposed/java_init.list 声明。</p>
 */
public class XposedEntry extends XposedModule {

    private static final String TAG = "VivoHealthMusicFix";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "模块已加载: process=" + param.getProcessName()
                + ", framework=" + getFrameworkName() + " " + getFrameworkVersion()
                + ", api=" + getApiVersion()
                + ", remote=" + ((getFrameworkProperties() & PROP_CAP_REMOTE) != 0));
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!Constants.TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        log(Log.INFO, TAG, "开始 Hook " + param.getPackageName());
        try {
            new MusicWhitelistHook(this).install(param.getClassLoader());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Hook 安装失败", t);
        }
    }
}
