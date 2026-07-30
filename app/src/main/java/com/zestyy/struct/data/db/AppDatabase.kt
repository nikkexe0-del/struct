package com.zestyy.struct.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.zestyy.struct.data.db.dao.RouteDao
import com.zestyy.struct.data.db.entities.RoutePointEntity
import com.zestyy.struct.data.db.entities.RouteMode
import com.zestyy.struct.data.db.entities.RouteType
import com.zestyy.struct.data.db.entities.SavedRouteEntity

class Converters {
    @TypeConverter
    fun fromRouteType(v: RouteType): String = v.name
    @TypeConverter
    fun toRouteType(v: String): RouteType = RouteType.valueOf(v)
    @TypeConverter
    fun fromRouteMode(v: RouteMode): String = v.name
    @TypeConverter
    fun toRouteMode(v: String): RouteMode = RouteMode.valueOf(v)
}

@Database(
    entities = [SavedRouteEntity::class, RoutePointEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "struct.db"
                )
                    // No migration path written yet — fine pre-release; once this ships to real
                    // users swap this for a proper Migration that adds offlineDownloadedAtMillis.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
