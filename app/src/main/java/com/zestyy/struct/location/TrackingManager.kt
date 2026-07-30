package com.zestyy.struct.location

import com.zestyy.struct.util.GeoMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackingState { IDLE, RECORDING, PAUSED }

data class TrackingSnapshot(
    val state: TrackingState = TrackingState.IDLE,
    val points: List<GeoMath.Point> = emptyList(),
    val lapMarkerIndices: Set<Int> = emptySet(),
    val distanceMeters: Double = 0.0,
    val elapsedMillis: Long = 0L,
    /** elapsed time actually moving, excludes auto-paused stationary time */
    val movingMillis: Long = 0L,
    val currentSpeedMps: Float = 0f,
    val elevationGain: Double = 0.0,
    val elevationLoss: Double = 0.0,
    val isAutoPaused: Boolean = false,
    /** heading of travel, used to rotate the "you are here" map sprite */
    val bearingDegrees: Double = 0.0
)

/**
 * Process-wide holder for the in-progress recording. The foreground [LocationTrackingService]
 * writes to this; Compose screens read it via [state]. Keeping it out of the service means the
 * UI keeps observing smoothly across service rebinds/process-visible states.
 */
object TrackingManager {
    private val _state = MutableStateFlow(TrackingSnapshot())
    val state: StateFlow<TrackingSnapshot> = _state.asStateFlow()

    private const val STATIONARY_SPEED_THRESHOLD_MPS = 0.4f // ~1.4 km/h
    private const val AUTO_PAUSE_AFTER_MS = 8_000L

    private var lastMotionAtMillis = 0L
    private var recordingStartedAtMillis = 0L
    private var accumulatedMovingMillis = 0L
    private var lastTickAtMillis = 0L

    fun start() {
        val now = System.currentTimeMillis()
        recordingStartedAtMillis = now
        lastTickAtMillis = now
        lastMotionAtMillis = now
        accumulatedMovingMillis = 0L
        _state.value = TrackingSnapshot(state = TrackingState.RECORDING)
    }

    fun resume() {
        val now = System.currentTimeMillis()
        lastTickAtMillis = now
        _state.value = _state.value.copy(state = TrackingState.RECORDING, isAutoPaused = false)
    }

    fun pause() {
        _state.value = _state.value.copy(state = TrackingState.PAUSED)
    }

    fun stop(): TrackingSnapshot {
        val finalSnapshot = _state.value
        _state.value = TrackingSnapshot()
        return finalSnapshot
    }

    fun addLapMarker() {
        val cur = _state.value
        if (cur.points.isEmpty()) return
        _state.value = cur.copy(lapMarkerIndices = cur.lapMarkerIndices + (cur.points.size - 1))
    }

    /** Called by the service on every accepted GPS fix. */
    fun onNewPoint(point: GeoMath.Point) {
        val cur = _state.value
        if (cur.state != TrackingState.RECORDING && cur.state != TrackingState.PAUSED) return

        val now = System.currentTimeMillis()
        val speed = point.speedMetersPerSec ?: 0f
        val moving = speed > STATIONARY_SPEED_THRESHOLD_MPS

        if (moving) lastMotionAtMillis = now
        val autoPaused = !moving && (now - lastMotionAtMillis) > AUTO_PAUSE_AFTER_MS

        // Don't record new points while auto-paused or manually paused (keeps distance accurate)
        if (cur.state == TrackingState.PAUSED || autoPaused) {
            _state.value = cur.copy(isAutoPaused = autoPaused)
            lastTickAtMillis = now
            return
        }

        val newPoints = cur.points + point
        val newDistance = if (cur.points.isNotEmpty())
            cur.distanceMeters + GeoMath.distanceMeters(cur.points.last(), point)
        else cur.distanceMeters

        accumulatedMovingMillis += (now - lastTickAtMillis).coerceAtLeast(0)
        lastTickAtMillis = now

        val (gain, loss) = if (newPoints.size % 5 == 0 || newPoints.size < 10)
            GeoMath.elevationGainLoss(newPoints) else cur.elevationGain to cur.elevationLoss

        val bearing = if (cur.points.isNotEmpty()) {
            val d = GeoMath.distanceMeters(cur.points.last(), point)
            // ignore near-zero moves so the sprite doesn't jitter/spin from GPS noise while
            // standing still — keep the last known heading instead
            if (d > 2.0) GeoMath.bearingDegrees(cur.points.last(), point) else cur.bearingDegrees
        } else cur.bearingDegrees

        _state.value = cur.copy(
            points = newPoints,
            distanceMeters = newDistance,
            elapsedMillis = now - recordingStartedAtMillis,
            movingMillis = accumulatedMovingMillis,
            currentSpeedMps = speed,
            elevationGain = gain,
            elevationLoss = loss,
            isAutoPaused = false,
            bearingDegrees = bearing
        )
    }

    /** Tick called ~once/sec by the service purely to keep elapsedMillis live in the UI. */
    fun tickClock() {
        val cur = _state.value
        if (cur.state != TrackingState.RECORDING) return
        _state.value = cur.copy(elapsedMillis = System.currentTimeMillis() - recordingStartedAtMillis)
    }
}
