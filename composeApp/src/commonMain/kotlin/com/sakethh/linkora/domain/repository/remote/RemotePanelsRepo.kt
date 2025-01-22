package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.Message
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.DeleteAPanelFromAFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import kotlinx.coroutines.flow.Flow

interface RemotePanelsRepo {
    suspend fun addANewPanel(addANewPanelDTO: AddANewPanelDTO): Flow<Result<NewItemResponseDTO>>
    suspend fun addANewFolderInAPanel(addANewPanelFolderDTO: AddANewPanelFolderDTO): Flow<Result<NewItemResponseDTO>>
    suspend fun deleteAPanel(id: Long): Flow<Result<Message>>
    suspend fun updateAPanelName(updatePanelNameDTO: UpdatePanelNameDTO): Flow<Result<Message>>
    suspend fun deleteAFolderFromAllPanels(folderID: Long): Flow<Result<Message>>
    suspend fun deleteAFolderFromAPanel(deleteAPanelFromAFolderDTO: DeleteAPanelFromAFolderDTO): Flow<Result<Message>>
}