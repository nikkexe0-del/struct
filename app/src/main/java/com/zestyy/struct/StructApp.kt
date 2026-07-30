package com.zestyy.struct

import android.app.Application
import com.zestyy.struct.ui.theme.ThemeManager
import org.osmdroid.config.Configuration
import java.io.File

class StructApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)

        // osmdroid needs a user agent + a writable cache dir for offline tile storage.
        // getExternalFilesDir() can return null (storage not yet mounted / some devices) —
        // falling back to internal filesDir avoids an NPE crash right at app startup.
        val baseDir = getExternalFilesDir("osmdroid") ?: File(filesDir, "osmdroid")
        baseDir.mkdirs()

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = baseDir
            osmdroidTileCache = File(baseDir, "tiles").apply { mkdirs() }

            // Perceived load-speed tuning: more parallel tile fetches, bigger in-memory cache so
            // panning doesn't keep re-hitting disk/network, and — the big one — treat any tile
            // already on disk as good enough to show immediately rather than waiting on a
            // network round-trip to confirm it's still fresh (OSM tiles barely change; this is
            // what makes re-opening a previously-viewed area feel instant instead of laggy).
            tileDownloadThreads = 6
            tileFileSystemThreads = 6
            tileDownloadMaxQueueSize = 40
            tileFileSystemMaxQueueSize = 40
            cacheMapTileCount = 18
            expirationOverrideDuration = 1000L * 60 * 60 * 24 * 14 // treat cached tiles as fresh for 14 days
        }
    }
}
