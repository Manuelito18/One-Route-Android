package com.onerouteandroid.oneroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onerouteandroid.oneroute.navigation.AppNavigation
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneRouteAndroidTheme {
                AppNavigation()
            }
        }
    }
}
