package com.Meditation.Sounds.frequencies.lemeor.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.AlbumsConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.CategoryConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.Converters
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.IntConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.PlaylistConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.PlaylistItemConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.ProgramConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.StringConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.TagConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.TierConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.converters.TrackConverter
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.AlbumDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.CategoryDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.HomeDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.PlaylistDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.ProgramDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.TagDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.TierDao
import com.Meditation.Sounds.frequencies.lemeor.data.database.dao.TrackDao
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import com.Meditation.Sounds.frequencies.lemeor.data.model.Category
import com.Meditation.Sounds.frequencies.lemeor.data.model.HomeResponse
import com.Meditation.Sounds.frequencies.lemeor.data.model.Playlist
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.data.model.Tag
import com.Meditation.Sounds.frequencies.lemeor.data.model.Tier
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track

@Database(entities = [
    HomeResponse::class,
    Tier::class,
    Category::class,
    Tag::class,
    Album::class,
    Track::class,
    Program::class,
    Playlist::class
], version = 2)

@TypeConverters(
        TierConverter::class,
        CategoryConverter::class,
        TagConverter::class,
        AlbumsConverter::class,
        PlaylistConverter::class,
        PlaylistItemConverter::class,
        ProgramConverter::class,
        TrackConverter::class,
        IntConverter::class,
        StringConverter::class,
        Converters::class
)
abstract class DataBase : RoomDatabase() {

    abstract fun homeDao(): HomeDao
    abstract fun tierDao(): TierDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun albumDao(): AlbumDao
    abstract fun trackDao(): TrackDao
    abstract fun programDao(): ProgramDao
    abstract fun playlistDao(): PlaylistDao

    companion object {

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE home ADD COLUMN tiers TEXT DEFAULT 0 NOT NULL")
                database.execSQL("CREATE TABLE IF NOT EXISTS tier (id INTEGER DEFAULT 0 NOT NULL, name TEXT DEFAULT 0 NOT NULL, `order` INTEGER DEFAULT 0 NOT NULL, updated_at INTEGER DEFAULT 0 NOT NULL, isShow INTEGER DEFAULT 0 NOT NULL, isPurchased INTEGER DEFAULT 0 NOT NULL, PRIMARY KEY(id))")
                database.execSQL("ALTER TABLE category ADD COLUMN tier_id INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE album ADD COLUMN tier_id INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE track ADD COLUMN tier_id INTEGER DEFAULT 0 NOT NULL")
            }
        }

        @Volatile private var instance: DataBase? = null

        fun getInstance(context: Context): DataBase =
                instance ?: synchronized(this) { instance ?: buildDatabase(context).also { instance = it } }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext, DataBase::class.java, BuildConfig.DB_NAME)
                        .allowMainThreadQueries()
                        .addMigrations(MIGRATION_1_2)
                        .build()
    }
}