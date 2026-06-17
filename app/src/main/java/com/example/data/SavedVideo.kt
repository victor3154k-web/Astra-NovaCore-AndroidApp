package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_videos")
data class SavedVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val localUri: String, // local file path, content URI, or URL
    val folderName: String = "Geral", // grouping for "Pastas" tab
    val addedMs: Long = System.currentTimeMillis(),
    val playbackPositionMs: Long = 0,
    val srtPath1: String? = null,
    val srtLang1: String? = null,
    val srtPath2: String? = null,
    val srtLang2: String? = null
)
