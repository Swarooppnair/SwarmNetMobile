package com.bitchat.android.features.security

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

enum class TrustEvent {
    MESSAGE_SENT,
    MESSAGE_RECEIVED,
    SPAM_DETECTED,
    MALICIOUS_BEHAVIOR,
    VERIFIED_IDENTITY,
    SUCCESSFUL_HANDSHAKE,
    FAILED_HANDSHAKE,
    CONSISTENT_BEHAVIOR,
    INCONSISTENT_BEHAVIOR
}

data class TrustProfile(
    var score: Float = 0.5f,
    var messagesSent: Int = 0,
    var messagesReceived: Int = 0,
    var spamCount: Int = 0,
    var maliciousCount: Int = 0,
    var verifiedIdentity: Boolean = false,
    var lastSeen: Long = System.currentTimeMillis(),
    var consistencyScore: Float = 1.0f
)

class TrustManager {
    
    companion object {
        private const val TAG = "TrustManager"
        private const val MIN_TRUST = 0.0f
        private const val MAX_TRUST = 1.0f
        private const val INITIAL_TRUST = 0.5f
        
        @Volatile
        private var instance: TrustManager? = null
        
        fun getInstance(): TrustManager {
            return instance ?: synchronized(this) {
                instance ?: TrustManager().also { instance = it }
            }
        }
    }
    
    private val trustProfiles = ConcurrentHashMap<String, TrustProfile>()
    private var enabled = true
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun getTrustScore(peerId: String): Float {
        if (!enabled) return INITIAL_TRUST
        return trustProfiles.getOrPut(peerId) { TrustProfile() }.score
    }
    
    fun updateTrust(peerId: String, event: TrustEvent) {
        if (!enabled) return
        
        val profile = trustProfiles.getOrPut(peerId) { TrustProfile() }
        
        when (event) {
            TrustEvent.MESSAGE_SENT -> {
                profile.messagesSent++
                adjustTrust(profile, 0.01f)
            }
            TrustEvent.MESSAGE_RECEIVED -> {
                profile.messagesReceived++
                adjustTrust(profile, 0.01f)
            }
            TrustEvent.SPAM_DETECTED -> {
                profile.spamCount++
                adjustTrust(profile, -0.15f)
                Log.w(TAG, "Spam detected from $peerId, trust: ${profile.score}")
            }
            TrustEvent.MALICIOUS_BEHAVIOR -> {
                profile.maliciousCount++
                adjustTrust(profile, -0.3f)
                Log.w(TAG, "Malicious behavior from $peerId, trust: ${profile.score}")
            }
            TrustEvent.VERIFIED_IDENTITY -> {
                profile.verifiedIdentity = true
                adjustTrust(profile, 0.2f)
            }
            TrustEvent.SUCCESSFUL_HANDSHAKE -> {
                adjustTrust(profile, 0.05f)
            }
            TrustEvent.FAILED_HANDSHAKE -> {
                adjustTrust(profile, -0.1f)
            }
            TrustEvent.CONSISTENT_BEHAVIOR -> {
                profile.consistencyScore = (profile.consistencyScore + 0.1f).coerceAtMost(1.0f)
                adjustTrust(profile, 0.02f)
            }
            TrustEvent.INCONSISTENT_BEHAVIOR -> {
                profile.consistencyScore = (profile.consistencyScore - 0.1f).coerceAtLeast(0.0f)
                adjustTrust(profile, -0.05f)
            }
        }
        
        profile.lastSeen = System.currentTimeMillis()
    }
    
    private fun adjustTrust(profile: TrustProfile, delta: Float) {
        profile.score = (profile.score + delta).coerceIn(MIN_TRUST, MAX_TRUST)
    }
    
    fun getTrustProfile(peerId: String): TrustProfile? = trustProfiles[peerId]
    
    fun isTrusted(peerId: String, threshold: Float = 0.6f): Boolean {
        return getTrustScore(peerId) >= threshold
    }
    
    fun getUntrustedPeers(threshold: Float = 0.3f): List<String> {
        return trustProfiles.filter { it.value.score < threshold }.map { it.key }
    }
    
    fun clearTrust(peerId: String) {
        trustProfiles.remove(peerId)
    }
}
