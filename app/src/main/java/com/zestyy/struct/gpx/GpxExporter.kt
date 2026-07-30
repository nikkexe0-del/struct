package com.zestyy.struct.gpx

import android.content.Context
import androidx.core.content.FileProvider
import com.zestyy.struct.data.db.entities.SavedRouteEntity
import com.zestyy.struct.util.GeoMath
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object GpxExporter {

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun export(context: Context, route: SavedRouteEntity, points: List<GeoMath.Point>): Uri {
        val dir = File(context.getExternalFilesDir(null), "gpx").apply { mkdirs() }
        val safeName = route.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "${safeName}_${route.id}.gpx")

        file.bufferedWriter().use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""" + "\n")
            w.write("""<gpx version="1.1" creator="maarga" xmlns="http://www.topografix.com/GPX/1/1">""" + "\n")
            w.write("  <trk>\n")
            w.write("    <name>${escape(route.name)}</name>\n")
            w.write("    <trkseg>\n")
            for (p in points) {
                w.write("      <trkpt lat=\"${p.lat}\" lon=\"${p.lng}\">\n")
                p.altitude?.let { w.write("        <ele>$it</ele>\n") }
                w.write("        <time>${iso.format(Date(p.timestampMillis ?: route.createdAtMillis))}</time>\n")
                w.write("      </trkpt>\n")
            }
            w.write("    </trkseg>\n")
            w.write("  </trk>\n")
            w.write("</gpx>\n")
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
