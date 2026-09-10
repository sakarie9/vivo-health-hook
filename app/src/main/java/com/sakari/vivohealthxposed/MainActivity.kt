package com.sakari.vivohealthxposed

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.sakari.vivohealthxposed.ui.WhitelistApp
import com.sakari.vivohealthxposed.ui.theme.VivoHealthXposedTheme

/** 应用入口：直接承载 Jetpack Compose 界面。 */
class MainActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpEdgeToEdge()
        setContent {
            VivoHealthXposedTheme {
                WhitelistApp()
            }
        }
    }

    /**
     * 尽可能彻底地打开 edge-to-edge：
     * 1. enableEdgeToEdge() 使用全透明系统栏（它的导航栏默认是半透明遮罩）；
     * 2. 再手动 setDecorFitsSystemWindows(false)，防止个别 ROM 忽略上一步；
     * 3. 直接把系统栏颜色写成透明，并关闭对比度遮罩；
     * 4. 系统栏图标明暗跟随应用主题。
     */
    @Suppress("DEPRECATION")
    private fun setUpEdgeToEdge() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // API < 35：由 enableEdgeToEdge 负责透明系统栏（默认导航栏是半透明遮罩，这里显式改成透明）。
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
        // 所有版本都强制内容铺满窗口。
        // API 35+ 不再调用 enableEdgeToEdge()：它会给 DecorView 注入 ColorProtection/ProtectionLayout
        // 对比度保护层，在部分设备的手势导航下会表现为底部一条黑色遮挡；Android 15 本身已强制 edge-to-edge。
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
