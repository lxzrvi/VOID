package dev.voidapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import dev.voidapp.ui.VoidRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // No translucent scrim behind the 3-button navigation bar: that grey/black
        // strip is what showed under pages on some phones.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        // The home screen is a full gradient; draw the bars over it.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // VoidRoot owns the theme: it reads the user's dark/light choice and
            // also drives the status bar icon colour, so there is never a strip.
            VoidRoot()
        }
    }
}
