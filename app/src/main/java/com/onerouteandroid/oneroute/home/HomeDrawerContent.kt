package com.onerouteandroid.oneroute.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onerouteandroid.oneroute.R

@Composable
fun HomeDrawerContent(
    onItemClick: (String) -> Unit = {},
    onCloseDrawer: () -> Unit = {}
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF6FF))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.user_person),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Mateo Hernández",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "mateo.h@ejemplo.com",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFF1D4ED8),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "✓ Verificado • Nivel 2",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Menu Options
            Text(
                text = "MENÚ PRINCIPAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Home,
                label = "Inicio",
                isSelected = true,
                onClick = {
                    onItemClick("Inicio")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Search,
                label = "Buscar Viaje",
                onClick = {
                    onItemClick("Buscar Viaje")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.DirectionsCar,
                label = "Mis Viajes",
                badge = "2 activos",
                onClick = {
                    onItemClick("Mis Viajes")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.History,
                label = "Historial de Viajes",
                onClick = {
                    onItemClick("Historial")
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
                color = Color.LightGray.copy(alpha = 0.4f)
            )

            Text(
                text = "MI CUENTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Eco,
                label = "Impacto Ecológico",
                onClick = {
                    onItemClick("Impacto Ecológico")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Payment,
                label = "Métodos de Pago",
                onClick = {
                    onItemClick("Métodos de Pago")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Outlined.Security,
                label = "Seguridad y Verificación",
                onClick = {
                    onItemClick("Seguridad")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = "Ayuda y Soporte",
                onClick = {
                    onItemClick("Ayuda")
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
                color = Color.LightGray.copy(alpha = 0.4f)
            )

            // Bottom Logout & App Info
            DrawerMenuItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = "Cerrar Sesión",
                tint = Color(0xFFDC2626),
                onClick = {
                    onItemClick("Cerrar Sesión")
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // OneRoute Branding Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.oneroute_logo_low),
                    contentDescription = "OneRoute",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OneRoute v1.0 • Movilidad Segura",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    badge: String? = null,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFDBEAFE) else Color.Transparent
    val contentColor = tint ?: if (isSelected) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Surface(
                    color = Color(0xFF1D4ED8),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
