package com.sakethh.linkora.domain.repository

import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import kotlinx.coroutines.flow.Flow

interface ExportDataRepo {
    /** This function is supposed to return raw data as JSON from the local database. */
    suspend fun rawExportDataAsJSON(): Flow<Result<RawExportString>>

    /** This function is supposed to return raw data as HTML from the local database. */
    suspend fun rawExportDataAsHTML(): Flow<Result<RawExportString>>

    /** exists to generate raw HTML from params rather than fetching it from the local database. */
    suspend fun rawExportDataAsHTML(
        links: List<Link>,
        folders: List<Folder>,
    ): RawExportString
}
