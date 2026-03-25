package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalizationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocalizedStrings(translation: List<LocalizedString>)

    @Query("SELECT EXISTS(SELECT * FROM localized_strings WHERE languageCode = :languageCode)")
    suspend fun doesStringsPackForThisLanguageExists(languageCode: String): Boolean

    @Query("DELETE FROM localized_strings WHERE languageCode=:languageCode")
    suspend fun deleteAllLocalizedStringsForThisLanguage(languageCode: String)

    @Query("SELECT stringValue FROM localized_strings WHERE stringName=:stringName and languageCode=:languageCode")
    suspend fun getLocalizedStringValueFor(stringName: String, languageCode: String): String?

    @Insert
    suspend fun addANewLanguage(localizedLanguage: LocalizedLanguage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNewLanguages(languages: List<LocalizedLanguage>)

    @Delete
    suspend fun deleteALanguage(localizedLanguage: LocalizedLanguage)

    @Query("DELETE FROM localized_languages WHERE languageName=:languageName")
    suspend fun deleteALanguage(languageName: String)

    @Query("DELETE FROM localized_languages WHERE languageCode=:languageCode")
    suspend fun deleteALanguageBasedOnLanguageCode(languageCode: String)


    @Query("SELECT languageName FROM localized_languages WHERE languageCode=:languageCode")
    suspend fun getLanguageNameForTheCode(languageCode: String): String

    @Query("SELECT languageCode FROM localized_languages WHERE languageName=:languageName")
    suspend fun getLanguageCodeForTheLanguageNamed(languageName: String): String

    @Query("SELECT * from localized_languages")
    fun getAllLanguages(): Flow<List<LocalizedLanguage>>
}