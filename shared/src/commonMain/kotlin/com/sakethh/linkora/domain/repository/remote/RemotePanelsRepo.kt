package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.DeleteAFolderFromAPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import kotlinx.coroutines.flow.Flow

interface RemotePanelsRepo {
    suspend fun addANewPanel(addANewPanelDTO: AddANewPanelDTO): Flow<Result<NewItemResponseDTO>>

    suspend fun addANewFolderInAPanel(
        addANewPanelFolderDTO: AddANewPanelFolderDTO,
    ): Flow<Result<NewItemResponseDTO>>

    suspend fun deleteAPanel(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateAPanelName(
        updatePanelNameDTO: UpdatePanelNameDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteAFolderFromAllPanels(
        idBasedDTO: IDBasedDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteAFolderFromAPanel(
        deleteAFolderFromAPanelDTO: DeleteAFolderFromAPanelDTO,
    ): Flow<Result<TimeStampBasedResponse>>
}
