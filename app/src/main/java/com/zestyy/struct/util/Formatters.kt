package com.zestyy.struct.util

object Formatters {
    fun km(meters: Double): String = "%.2f km".format(meters / 1000.0)

    fun meters(meters: Double): String = if (meters < 1000) "${meters.toInt()} m" else km(meters)

    fun duration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** pace in sec/km -> "5'12"/km" style string */
    fun pace(secPerKm: Double?): String {
        if (secPerKm == null || secPerKm.isInfinite() || secPerKm.isNaN()) return "--:--/km"
        val m = (secPerKm / 60).toInt()
        val s = (secPerKm % 60).toInt()
        return "%d'%02d\"/km".format(m, s)
    }

    fun speedKmh(mps: Float): String = "%.1f km/h".format(mps * 3.6)

    fun elevation(meters: Double): String = "${meters.toInt()} m"
}
