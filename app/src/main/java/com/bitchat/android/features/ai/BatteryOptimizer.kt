package com.bitchat.android.features.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryOptimizer(private val context: Context) {
    
    companion object {
        private const val DEFAULT_SCAN_INTERVAL = 10000L // 10 seconds
        private const val LOW_BATTERY_SCAN_INTERVAL = 30000L // 30 seconds
        private const val CRITICAL_BATTERY_SCAN_INTERVAL = 60000L // 1 minute
        
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
    }
    
    private var enabled = true
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun getOptimalScanInterval(): Long {
        if (!enabled) return DEFAULT_SCAN_INTERVAL
        
        val batteryLevel = getBatteryLevel()
        
        return when {
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> CRITICAL_BATTERY_SCAN_INTERVAL
            batteryLevel < LOW_BATTERY_THRESHOLD -> LOW_BATTERY_SCAN_INTERVAL
            else -> DEFAULT_SCAN_INTERVAL
        }
    }
    
    fun getOptimalRelayFrequency(): Int {
        if (!enabled) return 100 // 100% relay
        
        val batteryLevel = getBatteryLevel()
        
        return when {
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> 25 // Relay only 25% of messages
            batteryLevel < LOW_BATTERY_THRESHOLD -> 50 // Relay 50%
            else -> 100 // Relay all
        }
    }
    
    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100 // Default to full battery if unable to read
        }
    }
    
    fun isCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
}
