package com.bitchat.android.mapfeature

import android.content.Context
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitchat.android.features.sos.SosAlertStore
import com.bitchat.android.ui.ChatViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    chatViewModel: ChatViewModel? = null,
    onNavigateToBookmarks: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val bookmarks by viewModel.bookmarks.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val sosAlerts by SosAlertStore.alerts.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    // Init osmdroid config
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showAddBookmarkDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val center = mapView?.mapCenter
                    if (center != null && name.isNotBlank()) {
                        viewModel.addBookmark(name, desc, center.latitude, center.longitude)
                    }
                    showAddBookmarkDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    val mapController = controller
                    mapController.setZoom(15.0)
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                    mapController.setCenter(GeoPoint(0.0, 0.0))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                mapView = view

                // Remove old bookmark + SOS markers, keep location overlay
                view.overlays.removeAll { it is Marker }

                // Add bookmark markers
                bookmarks.forEach { bm ->
                    val marker = Marker(view)
                    marker.position = GeoPoint(bm.latitude, bm.longitude)
                    marker.title = bm.name
                    marker.snippet = bm.description
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    view.overlays.add(marker)
                }

                // Add SOS alert markers (red, with sender info)
                sosAlerts.forEach { sos ->
                    if (sos.latitude != 0.0 || sos.longitude != 0.0) {
                        val marker = Marker(view)
                        marker.position = GeoPoint(sos.latitude, sos.longitude)
                        marker.title = "🚨 SOS — @${sos.senderNickname}"
                        marker.snippet = "Geohash: #${sos.geohash}\nTime: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sos.timestamp))}"
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        view.overlays.add(marker)
                    }
                }

                view.invalidate()
            }
        )

        // SOS alert banner at top if any active alerts
        if (sosAlerts.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = Color(0xFFCC0000)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚨", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${sosAlerts.size} SOS alert${if (sosAlerts.size > 1) "s" else ""} — tap markers for details",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Floating controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    currentLocation?.let {
                        mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                    } ?: viewModel.requestFreshLocation()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.MyLocation, "Center on me")
            }

            FloatingActionButton(
                onClick = { showAddBookmarkDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.AddLocation, "Add bookmark at center")
            }

            FloatingActionButton(
                onClick = onNavigateToBookmarks,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.List, "Bookmarks list")
            }

            FloatingActionButton(
                onClick = { if (isTracking) viewModel.stopTracking() else viewModel.startTracking() },
                containerColor = if (isTracking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(if (isTracking) Icons.Default.StopCircle else Icons.Default.Terrain, "Toggle tracks")
            }
        }
    }
}
