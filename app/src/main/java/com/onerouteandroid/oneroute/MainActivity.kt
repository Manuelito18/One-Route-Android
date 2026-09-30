package com.onerouteandroid.oneroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.onerouteandroid.oneroute.home.HomeScaffold
import com.onerouteandroid.oneroute.liveNavigation.LiveNavScaffold
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneRouteAndroidTheme {
                var currentScreen by remember { mutableStateOf("home") }

                if (currentScreen == "home") {
                    HomeScaffold(
                        onNavigateToLiveNav = { currentScreen = "live_nav" }
                    )
                } else {
                    LiveNavScaffold(
                        onBackClick = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}
