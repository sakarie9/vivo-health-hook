package com.sakari.vivohealthxposed;

/**
 * 模块与 UI 共用的常量。
 *
 * <p>模块 App 与 vivo 健康进程之间通过 LSPosed 的 RemotePreferences 共享数据，
 * 组名 / key 必须两边一致。</p>
 */
public final class Constants {

    /** 被 Hook 的目标应用。与 META-INF/xposed/scope.list 保持一致。 */
    public static final String TARGET_PACKAGE = "com.vivo.health";

    /** RemotePreferences 组名。 */
    public static final String PREFS_GROUP = "music_whitelist";

    /** 存放包名集合的 key。 */
    public static final String KEY_PACKAGES = "packages";

    private Constants() {
    }
}
