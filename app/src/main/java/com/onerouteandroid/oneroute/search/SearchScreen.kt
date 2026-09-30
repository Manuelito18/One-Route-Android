package com.onerouteandroid.oneroute.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AirlineSeatReclineNormal
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onerouteandroid.oneroute.R
import com.onerouteandroid.oneroute.ui.theme.OneRouteAndroidTheme

data class SearchRideResult(
    val id: String,
    val driverName: String,
    val rating: String,
    val tripsCount: String,
    val isVerified: Boolean = false,
    val isTopDriver: Boolean = false,
    val price: String,
    val currencyUnit: String = "PEN / lugar",
    val departureTime: String,
    val departureLocation: String,
    val durationText: String,
    val routeViaText: String,
    val arrivalTime: String,
    val arrivalLocation: String,
    val carModel: String? = null,
    val availableSeats: Int,
    val hasAC: Boolean = false,
    val noSmoking: Boolean = false,
    val hasLargeTrunk: Boolean = false,
    val isLastSeat: Boolean = false
)

@Composable
fun SearchScreen(
    onRideSelect: (SearchRideResult) -> Unit = {},
    onViewMapClick: () -> Unit = {}
) {
    var originText by remember { mutableStateOf("Plaza de Armas, Chiclayo") }
    var destText by remember { mutableStateOf("Pimentel, Chiclayo") }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var passengerCount by remember { mutableIntStateOf(1) }
    var selectedSortIndex by remember { mutableIntStateOf(0) }

    val sampleSearchResults = remember {
        listOf(
            SearchRideResult(
                id = "1",
                driverName = "Roberto G.",
                rating = "4.9",
                tripsCount = "142 viajes",
                isVerified = true,
                price = "S/ 15",
                departureTime = "18:40",
                departureLocation = "Plaza de Armas",
                durationText = "45 min",
                routeViaText = "Vía Av. Bolognesi",
                arrivalTime = "19:25",
                arrivalLocation = "Pimentel Central",
                availableSeats = 2,
                hasAC = true,
                noSmoking = true
            ),
            SearchRideResult(
                id = "2",
                driverName = "Valeria P.",
                rating = "5.0",
                tripsCount = "88 viajes",
                isTopDriver = true,
                price = "S/ 12",
                departureTime = "19:00",
                departureLocation = "Av. Balta",
                durationText = "40 min",
                routeViaText = "Por Av. Chiclayo",
                arrivalTime = "19:40",
                arrivalLocation = "Mall Aventura",
                carModel = "Honda Civic 2022 • Gris",
                availableSeats = 1,
                isLastSeat = true
            ),
            SearchRideResult(
                id = "3",
                driverName = "Fernando R.",
                rating = "4.7",
                tripsCount = "54 viajes",
                price = "S/ 10",
                departureTime = "19:15",
                departureLocation = "USAT Chiclayo",
                durationText = "50 min",
                routeViaText = "Ruta Panamericana",
                arrivalTime = "20:05",
                arrivalLocation = "Lambayeque Central",
                availableSeats = 3,
                hasLargeTrunk = true
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Search Input Card
            item {
                SearchFilterCard(
                    originText = originText,
                    onOriginChange = { originText = it },
                    destText = destText,
                    onDestChange = { destText = it },
                    onSwapLocations = {
                        val temp = originText
                        originText = destText
                        destText = temp
                    },
                    selectedDayIndex = selectedDayIndex,
                    onDaySelect = { selectedDayIndex = it },
                    passengerCount = passengerCount,
                    onPassengerChange = { passengerCount = it }
                )
            }

            // Filter Pills Row
            item {
                SortChipsRow(
                    selectedIndex = selectedSortIndex,
                    onSelectIndex = { selectedSortIndex = it }
                )
            }

            // Results Counter Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sampleSearchResults.size} viajes disponibles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Actualizado en vivo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            // List of Ride Results
            items(sampleSearchResults) { ride ->
                SearchResultRideCard(
                    ride = ride,
                    onClick = { onRideSelect(ride) }
                )
            }
        }

        // Floating "Ver en mapa" Button
        ExtendedFloatingActionButton(
            onClick = onViewMapClick,
            containerColor = Color(0xFF0038A8),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    text = "Ver en mapa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}

@Composable
private fun SearchFilterCard(
    originText: String,
    onOriginChange: (String) -> Unit,
    destText: String,
    onDestChange: (String) -> Unit,
    onSwapLocations: () -> Unit,
    selectedDayIndex: Int,
    onDaySelect: (Int) -> Unit,
    passengerCount: Int,
    onPassengerChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Location Inputs with Timeline Indicator
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timeline dots & line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 6.dp, end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A)) // Green dot
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(Color.LightGray)
                        )
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFDC2626), // Red pin
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Inputs Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Origin Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Punto de partida",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    BasicTextField(
                                        value = originText,
                                        onValueChange = onOriginChange,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (originText.isEmpty()) {
                                                    Text(
                                                        text = "Punto de partida...",
                                                        fontSize = 15.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Mi ubicación",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Destination Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Destino",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    BasicTextField(
                                        value = destText,
                                        onValueChange = onDestChange,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (destText.isEmpty()) {
                                                    Text(
                                                        text = "Ingresa tu destino...",
                                                        fontSize = 15.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Swap Button floating right
                IconButton(
                    onClick = onSwapLocations,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE))
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Intercambiar",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Date Chips Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val days = listOf("Hoy", "Mañana", "Viernes 24")
                days.forEachIndexed { index, dayLabel ->
                    val isSelected = index == selectedDayIndex
                    Surface(
                        color = if (isSelected) Color(0xFF0038A8) else Color.White,
                        shape = CircleShape,
                        modifier = Modifier.clickable { onDaySelect(index) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = dayLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Passengers Counter & Schedule Box
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Passenger Count Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (passengerCount > 1) onPassengerChange(passengerCount - 1) },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Menos",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = passengerCount.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "pasajero",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(
                            onClick = { onPassengerChange(passengerCount + 1) },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Más",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Schedule Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Horario",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "18:00 - 19:30",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChipsRow(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val options = listOf("Menor precio", "Salida próxima", "Calificación 4.5+")
        options.forEachIndexed { index, optionLabel ->
            val isSelected = index == selectedIndex
            Surface(
                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFEFF6FF),
                shape = CircleShape,
                modifier = Modifier.clickable { onSelectIndex(index) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = optionLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF1E3A8A)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRideCard(
    ride: SearchRideResult,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Driver Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Image(
                            painter = painterResource(id = R.drawable.user_person),
                            contentDescription = "Foto de ${ride.driverName}",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (ride.isVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verificado",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ride.driverName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ride.isTopDriver) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Top Driver",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "★ ${ride.rating} • ${ride.tripsCount}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Price Column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ride.price,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1D4ED8)
                    )
                    Text(
                        text = ride.currencyUnit,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Route Timeline Section
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Departure Time & Location
                    Column {
                        Text(
                            text = ride.departureTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ride.departureLocation,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Route duration & line graphic
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = ride.durationText,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1D4ED8))
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF93C5FD)
                            )
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF93C5FD)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1D4ED8))
                            )
                        }
                        Text(
                            text = ride.routeViaText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }

                    // Arrival Time & Location
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ride.arrivalTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ride.arrivalLocation,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Car Model & Last Seat Alert
            if (ride.carModel != null || ride.isLastSeat) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ride.carModel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ride.carModel,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (ride.isLastSeat) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "❗ ¡Último asiento!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Amenities & Seat Count Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ride.hasAC) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AcUnit,
                                contentDescription = "A/C",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "A/C",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                if (ride.noSmoking) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SmokeFree,
                                contentDescription = "No fumar",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No fumar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                if (ride.hasLargeTrunk) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Luggage,
                                contentDescription = "Cajuela amplia",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cajuela amplia disponible",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFFDBEAFE),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AirlineSeatReclineNormal,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${ride.availableSeats} asientos disponibles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    OneRouteAndroidTheme {
        SearchScreen()
    }
}
