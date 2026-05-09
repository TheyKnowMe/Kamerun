package com.ME.kamerun.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val youtubeVideoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val thumbnailPath: String? = null,
    val audioPath: String? = null,
    val duration: Long? = null,
    val importedFrom: String,
    val importedAt: Long = System.currentTimeMillis(),
)
