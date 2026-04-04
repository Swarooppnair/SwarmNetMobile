package com.bitchat.android.features.map

import androidx.compose.ui.graphics.Color

data class DeviceMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val nickname: String,
    val isConnected: Boolean,
    val trustScore: Float
)

data class ConnectionLine(
    val fromId: String,
    val toId: String,
    val strength: Float,
    val latency: Long
)

data class RouteHighlight(
    val path: List<String>, // List of peer IDs
    val color: Color,
    val isActive: Boolean
)

interface MeshMapOverlay {
    fun addDeviceMarker(marker: DeviceMarker)
    fun removeDeviceMarker(id: String)
    fun updateDeviceMarker(marker: DeviceMarker)
    fun getDeviceMarkers(): List<DeviceMarker>
    
    fun addConnectionLine(line: ConnectionLine)
    fun removeConnectionLine(fromId: String, toId: String)
    fun getConnectionLines(): List<ConnectionLine>
    
    fun highlightRoute(route: RouteHighlight)
    fun clearRouteHighlights()
    
    fun centerOnDevice(id: String)
    fun fitAllDevices()
}

class MeshMapOverlayImpl : MeshMapOverlay {
    private val deviceMarkers = mutableMapOf<String, DeviceMarker>()
    private val connectionLines = mutableListOf<ConnectionLine>()
    private val routeHighlights = mutableListOf<RouteHighlight>()
    
    override fun addDeviceMarker(marker: DeviceMarker) {
        deviceMarkers[marker.id] = marker
    }
    
    override fun removeDeviceMarker(id: String) {
        deviceMarkers.remove(id)
    }
    
    override fun updateDeviceMarker(marker: DeviceMarker) {
        deviceMarkers[marker.id] = marker
    }
    
    override fun getDeviceMarkers(): List<DeviceMarker> = deviceMarkers.values.toList()
    
    override fun addConnectionLine(line: ConnectionLine) {
        connectionLines.removeIf { it.fromId == line.fromId && it.toId == line.toId }
        connectionLines.add(line)
    }
    
    override fun removeConnectionLine(fromId: String, toId: String) {
        connectionLines.removeIf { it.fromId == fromId && it.toId == toId }
    }
    
    override fun getConnectionLines(): List<ConnectionLine> = connectionLines.toList()
    
    override fun highlightRoute(route: RouteHighlight) {
        routeHighlights.add(route)
    }
    
    override fun clearRouteHighlights() {
        routeHighlights.clear()
    }
    
    override fun centerOnDevice(id: String) {
        // To be implemented with actual map SDK
    }
    
    override fun fitAllDevices() {
        // To be implemented with actual map SDK
    }
}
