package com.sakari.vivohealthxposed;

import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * 把用户在模块 App 里填写的包名注入到 vivo 健康的音乐白名单。
 *
 * <p>反编译定位（vivo 健康 7.1.1.02）：</p>
 * <pre>
 * MusicCtrlManager 的静态初始化里硬编码了约 100 个包名，存到静态字段 d（appList）。
 * MusicCtrlManager$Companion.b()  = getAppList()，返回字段 d。
 * MusicCtrlManager$Companion.m(packageName, mediaControllers) = updateMediaControl()
 *      -&gt; 调用 b() 取出白名单
 *      -&gt; 调用 k(packageName, controllers, whitelist) = selectController()
 *      -&gt; 只保留 whitelist.contains(controller.getPackageName()) 的 MediaController
 * 若白名单里没有当前播放音乐的包名，selectController 返回 null，
 * 手机就不会把播放状态推给手表，手表于是显示“请先在手机播放音乐”。
 *
 * <p>因此这里只 Hook 白名单的读取入口 b()，在它返回的 List 后面追加用户包名。
 * 该入口是应用中读取白名单的唯一路径（access$getAppList$cp 仅被 b() 调用），
 * 所以这一处注入即可覆盖音乐控制、MusicModule、MusicModuleUtil 等全部使用点。</p>
 */
final class MusicWhitelistHook {

    private static final String TAG = "VivoHealthMusicFix";

    private static final String MANAGER_CLASS =
            "com.vivo.health.devices.watch.musiccontrol.MusicCtrlManager";
    private static final String COMPANION_CLASS = MANAGER_CLASS + "$Companion";

    /** 已知的 getAppList 方法名：Companion 里是 b()，外层类的合成访问器。 */
    private static final String[] PREFERRED_NAMES = {"b", "access$getAppList$cp"};

    private final XposedModule mModule;

    private volatile SharedPreferences mRemotePrefs;
    private volatile Set<String> mLastInjected = Collections.emptySet();

    MusicWhitelistHook(XposedModule module) {
        mModule = module;
    }

    void install(ClassLoader loader) {
        Class<?> owner = null;
        Method target = null;

        Class<?> companion = loadClass(COMPANION_CLASS, loader);
        if (companion != null) {
            target = findAppListGetter(companion);
            if (target != null) {
                owner = companion;
            }
        }
        if (target == null) {
            Class<?> manager = loadClass(MANAGER_CLASS, loader);
            if (manager != null) {
                target = findAppListGetter(manager);
                if (target != null) {
                    owner = manager;
                }
            }
        }

        if (target == null) {
            mModule.log(Log.ERROR, TAG, "未找到音乐白名单读取方法，注入未生效。"
                    + "请确认 vivo 健康版本（当前适配 7.1.1.02）。");
            return;
        }

        final String where = owner.getName() + "#" + target.getName() + "()";
        target.setAccessible(true);

        mModule.hook(target)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object original = chain.proceed();
                    Set<String> extra = readUserPackages();
                    if (extra.isEmpty()) {
                        return original;
                    }

                    LinkedHashSet<String> merged = new LinkedHashSet<>();
                    if (original instanceof List) {
                        for (Object item : (List<?>) original) {
                            if (item instanceof String) {
                                merged.add((String) item);
                            }
                        }
                    }
                    merged.addAll(extra);

                    if (!merged.equals(mLastInjected)) {
                        mLastInjected = merged;
                        mModule.log(Log.INFO, TAG, "已注入音乐白名单 (" + where + ")，共 "
                                + merged.size() + " 项，其中自定义 " + extra.size() + " 项: " + extra);
                    }
                    return new ArrayList<>(merged);
                });

        mModule.log(Log.INFO, TAG, "Hook 成功: " + where);
    }

    private Set<String> readUserPackages() {
        try {
            SharedPreferences prefs = mRemotePrefs;
            if (prefs == null) {
                prefs = mModule.getRemotePreferences(Constants.PREFS_GROUP);
                mRemotePrefs = prefs;
            }
            Set<String> raw = prefs.getStringSet(Constants.KEY_PACKAGES, null);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptySet();
            }
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (String value : raw) {
                if (value == null) {
                    continue;
                }
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        } catch (Throwable t) {
            mModule.log(Log.ERROR, TAG, "读取远程配置失败", t);
            return Collections.emptySet();
        }
    }

    private static Class<?> loadClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 按签名查找 getAppList：无参、返回 {@link List}。
     * 优先匹配已知方法名，其次在该签名唯一时才采用，避免误 Hook。
     */
    private static Method findAppListGetter(Class<?> owner) {
        List<Method> candidates = new ArrayList<>();
        Method preferred = null;
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getParameterCount() != 0
                    || !List.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            candidates.add(method);
            for (String name : PREFERRED_NAMES) {
                if (name.equals(method.getName())) {
                    preferred = method;
                }
            }
        }
        if (preferred != null) {
            return preferred;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }
}
