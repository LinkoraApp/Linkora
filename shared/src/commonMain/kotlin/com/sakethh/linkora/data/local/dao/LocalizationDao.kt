package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalizationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocalizedStrings(translations: List<LocalizedString>)

    @Query("SELECT EXISTS(SELECT * FROM localized_strings WHERE languageCode = :languageCode)")
    suspend fun doesStringsPackForThisLanguageExists(languageCode: String): Boolean

    @Query("DELETE FROM localized_strings WHERE languageCode=:languageCode")
    suspend fun deleteAllLocalizedStringsForThisLanguage(languageCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNewLanguages(languages: List<LocalizedLanguage>)

    @Query("SELECT * from localized_languages")
    fun getAllLanguages(): Flow<List<LocalizedLanguage>>

    @Query("SELECT stringName, stringValue FROM localized_strings WHERE languageCode = :languageCode")
    suspend fun getLocalizedPairs(
        languageCode: String
    ): Map<
        @MapColumn(columnName = "stringName")
        String,
        @MapColumn(columnName = "stringValue")
        String
        >
}
