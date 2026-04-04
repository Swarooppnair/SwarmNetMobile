package com.bitchat.android.features.sos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SosAlert(
    val id: String,
    val senderNickname: String,
    val latitude: Double,
    val longitude: Double,
    val geohash: String,
    val timestamp: Long
)

object SosAlertStore {
    private val _alerts = MutableStateFlow<List<SosAlert>>(emptyList())
    val alerts: StateFlow<List<SosAlert>> = _alerts.asStateFlow()

    fun addAlert(alert: SosAlert) {
        _alerts.value = (_alerts.value + alert).takeLast(50)
    }

    fun clearAll() {
        _alerts.value = emptyList()
    }
}
