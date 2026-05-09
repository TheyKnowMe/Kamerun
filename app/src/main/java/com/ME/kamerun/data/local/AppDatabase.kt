package com.ME.kamerun.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.data.local.entities.PlaylistSongCrossRef
import com.ME.kamerun.data.local.entities.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}