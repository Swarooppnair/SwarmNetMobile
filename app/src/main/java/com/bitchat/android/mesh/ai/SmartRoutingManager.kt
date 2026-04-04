package com.bitchat.android.mesh.ai

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

data class Peer(
    val id: String,
    val rssi: Int = 0,
    val successRate: Float = 1.0f,
    val latency: Long = 0L,
    val uptime: Long = 0L,
    val trustScore: Float = 1.0f
)

data class PeerMetrics(
    var totalAttempts: Int = 0,
    var successfulAttempts: Int = 0,
    var totalLatency: Long = 0L,
    var lastSeen: Long = System.currentTimeMillis(),
    var rssiHistory: MutableList<Int> = mutableListOf(),
    var trustScore: Float = 1.0f
)

class SmartRoutingManager {
    
    companion object {
        private const val TAG = "SmartRoutingAI"
        private const val MAX_RSSI_HISTORY = 10
        
        @Volatile
        private var instance: SmartRoutingManager? = null
        
        fun getInstance(): SmartRoutingManager {
            return instance ?: synchronized(this) {
                instance ?: SmartRoutingManager().also { instance = it }
            }
        }
    }
    
    private val peerMetrics = ConcurrentHashMap<String, PeerMetrics>()
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled
    
    // Adaptive weights (learned over time)
    private var rssiWeight = 0.3f
    private var successWeight = 0.4f
    private var latencyWeight = 0.2f
    private var trustWeight = 0.1f
    
    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }
    
    fun getBestNextHop(peers: List<Peer>): Peer? {
        if (!_enabled.value || peers.isEmpty()) return peers.firstOrNull()
        
        return peers.maxByOrNull { peer ->
            val metrics = peerMetrics[peer.id]
            val score = calculatePeerScore(peer, metrics)
            Log.d(TAG, "Peer ${peer.id} score: $score")
            score
        }
    }
    
    private fun calculatePeerScore(peer: Peer, metrics: PeerMetrics?): Float {
        val rssiScore = normalizeRSSI(peer.rssi)
        val successScore = metrics?.let {
            if (it.totalAttempts > 0) it.successfulAttempts.toFloat() / it.totalAttempts else 1.0f
        } ?: 1.0f
        val latencyScore = normalizeLatency(metrics?.let {
            if (it.successfulAttempts > 0) it.totalLatency / it.successfulAttempts else 100L
        } ?: 100L)
        val trustScore = metrics?.trustScore ?: 1.0f
        
        return (rssiScore * rssiWeight) +
               (successScore * successWeight) +
               (latencyScore * latencyWeight) +
               (trustScore * trustWeight)
    }
    
    private fun normalizeRSSI(rssi: Int): Float {
        // RSSI typically ranges from -100 (weak) to -30 (strong)
        return ((rssi + 100).coerceIn(0, 70) / 70f).coerceIn(0f, 1f)
    }
    
    private fun normalizeLatency(latency: Long): Float {
        // Lower latency is better; normalize 0-1000ms range
        return (1.0f - (latency.coerceIn(0, 1000) / 1000f)).coerceIn(0f, 1f)
    }
    
    fun updatePeerMetrics(peerId: String, success: Boolean, latency: Long, rssi: Int = 0) {
        val metrics = peerMetrics.getOrPut(peerId) { PeerMetrics() }
        
        metrics.totalAttempts++
        if (success) {
            metrics.successfulAttempts++
            metrics.totalLatency += latency
        }
        metrics.lastSeen = System.currentTimeMillis()
        
        if (rssi != 0) {
            metrics.rssiHistory.add(rssi)
            if (metrics.rssiHistory.size > MAX_RSSI_HISTORY) {
                metrics.rssiHistory.removeAt(0)
            }
        }
        
        // Adaptive learning: adjust weights based on success patterns
        if (metrics.totalAttempts % 10 == 0) {
            adaptWeights(metrics)
        }
    }
    
    private fun adaptWeights(metrics: PeerMetrics) {
        val successRate = metrics.successfulAttempts.toFloat() / metrics.totalAttempts
        
        // If success rate is low, increase trust weight
        if (successRate < 0.7f) {
            trustWeight = (trustWeight + 0.05f).coerceAtMost(0.4f)
            successWeight = (successWeight - 0.05f).coerceAtLeast(0.2f)
        }
        
        // Normalize weights to sum to 1.0
        val sum = rssiWeight + successWeight + latencyWeight + trustWeight
        rssiWeight /= sum
        successWeight /= sum
        latencyWeight /= sum
        trustWeight /= sum
    }
    
    fun updateTrustScore(peerId: String, trustScore: Float) {
        peerMetrics.getOrPut(peerId) { PeerMetrics() }.trustScore = trustScore.coerceIn(0f, 1f)
    }
    
    fun getPeerMetrics(peerId: String): PeerMetrics? = peerMetrics[peerId]
    
    fun clearMetrics() {
        peerMetrics.clear()
    }
}
