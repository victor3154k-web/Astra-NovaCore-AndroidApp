package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM saved_videos ORDER BY addedMs DESC")
    fun getAllVideos(): Flow<List<SavedVideo>>

    @Query("SELECT * FROM saved_videos WHERE id = :id")
    suspend fun getVideoById(id: Int): SavedVideo?

    @Query("SELECT * FROM saved_videos WHERE localUri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): SavedVideo?

    @Query("SELECT * FROM saved_videos")
    suspend fun getAllVideosDirect(): List<SavedVideo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: SavedVideo): Long

    @Update
    suspend fun updateVideo(video: SavedVideo)

    @Delete
    suspend fun deleteVideo(video: SavedVideo)
}
