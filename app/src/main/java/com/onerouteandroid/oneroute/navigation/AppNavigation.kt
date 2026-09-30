package com.onerouteandroid.oneroute.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onerouteandroid.oneroute.components.ComingSoonScreen
import com.onerouteandroid.oneroute.home.HomeBottomBar
import com.onerouteandroid.oneroute.home.HomeDrawerContent
import com.onerouteandroid.oneroute.home.HomeScreen
import com.onerouteandroid.oneroute.home.HomeTopBar
import com.onerouteandroid.oneroute.liveNavigation.LiveNavScaffold
import com.onerouteandroid.oneroute.ridedetails.RideDetailScreen
import com.onerouteandroid.oneroute.search.SearchScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                currentRoute = currentRoute,
                onItemClick = { destination ->
                    when (destination) {
                        "Inicio" -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                        "Buscar Viaje" -> navController.navigate(Screen.Search.route)
                        "Mis Viajes" -> navController.navigate(Screen.MyRides.route)
                        "Perfil" -> navController.navigate(Screen.Profile.route)
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
                if (currentRoute != Screen.LiveNav.route && currentRoute != Screen.RideDetail.route) {
                    val titleText = when (currentRoute) {
                        Screen.Search.route -> "Buscar"
                        Screen.MyRides.route -> "Mis Viajes"
                        Screen.Profile.route -> "Perfil"
                        else -> "Inicio"
                    }

                    HomeTopBar(
                        title = titleText,
                        onLogoClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onNotificationClick = { /* Notificaciones */ },
                        onProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }
            },
            bottomBar = {
                if (currentRoute != Screen.LiveNav.route && currentRoute != Screen.RideDetail.route) {
                    val selectedIndex = when (currentRoute) {
                        Screen.Home.route -> 0
                        Screen.Search.route -> 1
                        Screen.MyRides.route -> 2
                        Screen.Profile.route -> 3
                        else -> 0
                    }

                    HomeBottomBar(
                        selectedTab = selectedIndex,
                        onTabSelected = { tab ->
                            val route = when (tab) {
                                0 -> Screen.Home.route
                                1 -> Screen.Search.route
                                2 -> Screen.MyRides.route
                                3 -> Screen.Profile.route
                                else -> Screen.Home.route
                            }
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onSearchClick = { navController.navigate(Screen.Search.route) },
                        onSeeAllRidesClick = { navController.navigate(Screen.Search.route) },
                        onRideClick = { _ -> navController.navigate(Screen.RideDetail.route) }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onRideSelect = { _ -> navController.navigate(Screen.RideDetail.route) },
                        onViewMapClick = { navController.navigate(Screen.LiveNav.route) }
                    )
                }

                composable(Screen.RideDetail.route) {
                    RideDetailScreen(
                        onBackClick = { navController.popBackStack() },
                        onReserveClick = { navController.navigate(Screen.LiveNav.route) }
                    )
                }

                composable(Screen.LiveNav.route) {
                    LiveNavScaffold(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.MyRides.route) {
                    ComingSoonScreen(
                        title = "Mis Viajes",
                        message = "Próximamente podrás ver y gestionar todos tus viajes reservados y publicados."
                    )
                }

                composable(Screen.Profile.route) {
                    ComingSoonScreen(
                        title = "Perfil",
                        message = "Próximamente podrás editar tu perfil, métodos de pago y configuraciones de cuenta."
                    )
                }
            }
        }
    }
}
