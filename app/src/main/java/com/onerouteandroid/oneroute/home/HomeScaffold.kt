package com.onerouteandroid.oneroute.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun HomeScaffold(
    onNavigateToLiveNav: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                onItemClick = { item ->
                    if (item == "Mis Viajes" || item == "Inicio") {
                        // Handle item click
                    }
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    onLogoClick = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onNotificationClick = { /* Notificaciones */ },
                    onProfileClick = { /* Perfil */ }
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex -> selectedTab = tabIndex }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { /* Publicar viaje */ },
                    containerColor = Color(0xFF0038A8),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Publicar viaje"
                        )
                    },
                    text = {
                        Text(
                            text = "Publicar viaje",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HomeScreen(
                    onSearchClick = onNavigateToLiveNav,
                    onSeeAllRidesClick = { /* Ver todos */ },
                    onRideClick = { _ ->
                        onNavigateToLiveNav()
                    }
                )
            }
        }
    }
}
