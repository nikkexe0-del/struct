package com.zestyy.struct.gpx

import android.content.Context
import android.net.Uri
import com.zestyy.struct.util.GeoMath
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.*

object GpxImporter {

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    data class ParsedGpx(val name: String?, val points: List<GeoMath.Point>)

    fun import(context: Context, uri: Uri): ParsedGpx {
        val parser = XmlPullParserFactoryHolder.newPullParser()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open GPX file" }
            parser.setInput(input, null)

            var trackName: String? = null
            val points = mutableListOf<GeoMath.Point>()

            var eventType = parser.eventType
            var curLat = 0.0
            var curLng = 0.0
            var curEle: Double? = null
            var curTime: Long? = null
            var inTrkpt = false
            var inName = false
            var text = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "trkpt", "wpt" -> {
                            inTrkpt = true
                            curLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            curLng = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                            curEle = null
                            curTime = null
                        }
                        "name" -> inName = true
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "ele" -> if (inTrkpt) curEle = text.trim().toDoubleOrNull()
                        "time" -> if (inTrkpt) curTime = parseTime(text.trim())
                        "name" -> {
                            if (inName && trackName == null) trackName = text.trim()
                            inName = false
                        }
                        "trkpt", "wpt" -> {
                            points.add(GeoMath.Point(curLat, curLng, curEle, curTime))
                            inTrkpt = false
                        }
                    }
                }
                eventType = parser.next()
            }
            return ParsedGpx(trackName, points)
        }
    }

    private fun parseTime(raw: String): Long? = try {
        iso.parse(raw.removeSuffix("Z"))?.time
    } catch (e: Exception) {
        null
    }
}

/** Small indirection so this file doesn't need an extra import block at the top for the factory. */
private object XmlPullParserFactoryHolder {
    fun newPullParser(): XmlPullParser =
        org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
}
