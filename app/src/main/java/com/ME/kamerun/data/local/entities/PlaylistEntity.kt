package com.ME.kamerun.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vibe: String? = null,
    val isAiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
