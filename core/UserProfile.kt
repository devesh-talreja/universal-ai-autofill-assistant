package com.example.smartautofiller.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Section model ─────────────────────────────────────────────
@Serializable
data class ProfileSection(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val icon: String = "📋",
    val fields: List<SectionField> = emptyList()
)

@Serializable
data class SectionField(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val value: String = ""
)

// ── User Profile ──────────────────────────────────────────────
@Serializable
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "profile_name") val profileName: String = "",
    @ColumnInfo(name = "full_name") val fullName: String = "",
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "phone_number") val phoneNumber: String = "",
    @ColumnInfo(name = "address") val address: String = "",
    @ColumnInfo(name = "custom_fields") val customFields: Map<String, String> = emptyMap(),
    @ColumnInfo(name = "sections") val sections: List<ProfileSection> = emptyList()
)

// ── Type Converters ───────────────────────────────────────────
class CustomFieldsConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromCustomFields(value: Map<String, String>): String =
        try { json.encodeToString(value) } catch (e: Exception) { "{}" }

    @TypeConverter
    fun toCustomFields(value: String): Map<String, String> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyMap() }

    @TypeConverter
    fun fromSections(value: List<ProfileSection>): String =
        try { json.encodeToString(value) } catch (e: Exception) { "[]" }

    @TypeConverter
    fun toSections(value: String): List<ProfileSection> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }
}