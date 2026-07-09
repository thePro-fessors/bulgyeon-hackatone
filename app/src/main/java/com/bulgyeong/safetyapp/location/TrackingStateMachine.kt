package com.bulgyeong.safetyapp.location

class TrackingStateMachine(
    private val listener: Listener
) {
    interface Listener {
        fun onStateChanged(state: TrackingState)
        fun onRequestBleScan()
        fun onRequestPdrStart()
        fun onAnchorDetected(anchorLat: Double, anchorLon: Double)
    }

    private var currentState: TrackingState = TrackingState.GPS_ACTIVE
    private var lastBleAnchorTimestamp: Long = 0L
    private val bleStaleThresholdMs = 30000L
    private val gpsAccuracyThreshold = 40f

    fun start() {
        currentState = TrackingState.GPS_ACTIVE
        listener.onStateChanged(currentState)
    }

    fun onGpsLocation(locationAccuracy: Float?) {
        if (locationAccuracy != null && locationAccuracy <= gpsAccuracyThreshold) {
            transitionTo(TrackingState.GPS_ACTIVE)
            return
        }
        if (currentState == TrackingState.GPS_ACTIVE) {
            transitionTo(TrackingState.BLE_UWB_ACTIVE)
        }
    }

    fun onGpsLost() {
        if (currentState == TrackingState.GPS_ACTIVE) {
            transitionTo(TrackingState.BLE_UWB_ACTIVE)
        }
    }

    fun onBleReadings(readings: List<BeaconReading>) {
        val anchor = readings.firstOrNull { it.latitude != null && it.longitude != null }
        if (anchor != null) {
            lastBleAnchorTimestamp = System.currentTimeMillis()
            if (currentState == TrackingState.PDR_ACTIVE) {
                listener.onAnchorDetected(anchor.latitude!!, anchor.longitude!!)
            }
            transitionTo(TrackingState.BLE_UWB_ACTIVE)
            return
        }

        if (currentState != TrackingState.GPS_ACTIVE) {
            if (System.currentTimeMillis() - lastBleAnchorTimestamp > bleStaleThresholdMs) {
                transitionTo(TrackingState.PDR_ACTIVE)
            } else {
                transitionTo(TrackingState.BLE_UWB_ACTIVE)
            }
        }
    }

    fun onPdrMotion() {
        if (currentState == TrackingState.BLE_UWB_ACTIVE && System.currentTimeMillis() - lastBleAnchorTimestamp > bleStaleThresholdMs) {
            transitionTo(TrackingState.PDR_ACTIVE)
        }
    }

    private fun transitionTo(newState: TrackingState) {
        if (newState == currentState) {
            if (newState == TrackingState.BLE_UWB_ACTIVE) {
                listener.onRequestBleScan()
            }
            return
        }
        currentState = newState
        listener.onStateChanged(currentState)
        when (currentState) {
            TrackingState.GPS_ACTIVE -> {
                // GPS가 복구되면 BLE/PDR 중지
            }
            TrackingState.BLE_UWB_ACTIVE -> {
                listener.onRequestBleScan()
            }
            TrackingState.PDR_ACTIVE -> {
                listener.onRequestPdrStart()
            }
        }
    }
}
