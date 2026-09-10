package com.sakari.vivohealthxposed;

import android.app.Application;

import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * 持有 LSPosed 框架服务的 Application。
 *
 * <p>libxposed 的现代 API 中，模块 App 不再被 Hook，而是由框架通过
 * 模块自身的 XposedService ContentProvider 把服务 binder 发给我们。
 * 这里注册一次监听，并把服务状态广播给界面。</p>
 */
public class ModuleApp extends Application {

    public interface ServiceStateListener {
        void onServiceChanged(XposedService service);
    }

    private static final CopyOnWriteArrayList<ServiceStateListener> LISTENERS =
            new CopyOnWriteArrayList<>();

    private static volatile XposedService sService;

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                sService = service;
                notifyListeners();
            }

            @Override
            public void onServiceDied(XposedService service) {
                if (sService == service) {
                    sService = null;
                }
                notifyListeners();
            }
        });
    }

    public static XposedService getService() {
        return sService;
    }

    public static void addListener(ServiceStateListener listener) {
        LISTENERS.addIfAbsent(listener);
    }

    public static void removeListener(ServiceStateListener listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        XposedService service = sService;
        for (ServiceStateListener listener : LISTENERS) {
            listener.onServiceChanged(service);
        }
    }
}
