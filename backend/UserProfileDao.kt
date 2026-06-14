package com.example.smartautofiller.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    // Export ke liye - suspend function (one-shot)
    @Query("SELECT * FROM user_profiles ORDER BY id ASC")
    suspend fun getAllProfilesList(): List<UserProfile>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Delete
    suspend fun deleteProfile(profile: UserProfile)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllProfiles()
}