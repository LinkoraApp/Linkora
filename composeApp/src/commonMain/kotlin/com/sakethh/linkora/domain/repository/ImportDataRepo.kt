package com.sakethh.linkora.domain.repository

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.JSONExportSchema
import kotlinx.coroutines.flow.Flow

interface ImportDataRepo {
    suspend fun importDataFromObj(jsonExportSchema: JSONExportSchema): Flow<Result<Unit>>

    suspend fun importDataFromHTML(html: String): Flow<Result<Unit>>
}