package com.sakethh.linkora.domain

import androidx.compose.runtime.Composable
import com.sakethh.linkora.domain.dto.server.ArchiveMultipleItemsDTO

typealias ComposableContent = @Composable () -> Unit

typealias RawExportString = String

typealias ImportFileType = FileType

typealias ExportFileType = FileType

typealias DeleteMultipleItemsDTO = ArchiveMultipleItemsDTO
