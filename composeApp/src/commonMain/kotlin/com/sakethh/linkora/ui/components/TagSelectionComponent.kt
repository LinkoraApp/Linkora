package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.asUnifiedLazyState
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagSelectionComponent(
    paddingValues: PaddingValues = PaddingValues(start = 15.dp, end = 25.dp, top = 5.dp),
    allTags: PaginationState<Map<Pair<LastSeenId, LastSeenString>, List<Tag>>>,
    selectedTags: List<Tag>,
    onTagClick: (tag: Tag) -> Unit,
    onRetrieveNextTagsPage: () -> Unit,
    onFirstVisibleIndexChange: (Int) -> Unit,
    showCreateNewTagSheet: () -> Unit
) {
    val lazyColumnState = rememberLazyListState()
    val lazyColumnUnifiedState = retain {
        lazyColumnState.asUnifiedLazyState()

    }
    val isTagsEmpty = allTags.data.isEmpty() || allTags.data.values.first().isEmpty()

    val buttonTotalHeight = ButtonDefaults.MinHeight + 30.dp
    val buttonOverlapAmount = 25.dp
    val buttonVisibleHeight = buttonTotalHeight - buttonOverlapAmount

    PerformAtTheEndOfTheList(
        unifiedLazyState = lazyColumnUnifiedState,
        actionOnReachingEnd = onRetrieveNextTagsPage
    )

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazyColumnState.firstVisibleItemIndex
        }.debounce(500).distinctUntilChanged().collect {
            onFirstVisibleIndexChange(it)
        }
    }
    Box(
        modifier = Modifier.animateContentSize()
            .heightIn(max = 345.dp)
            .fillMaxWidth()
    ) {
        OutlinedButton(
            shape = RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
                .fillMaxWidth()
                .height(buttonTotalHeight)
                .pointerHoverIcon(icon = PointerIcon.Hand),
            onClick = {
                showCreateNewTagSheet()
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 5.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = Localization.Key.CreateANewTag.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
            }
        }
        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier.zIndex(2f).fillMaxWidth().heightIn(min = 75.dp, max = 345.dp)
                .padding(bottom = buttonVisibleHeight).padding(
                    paddingValues
                ).clip(RoundedCornerShape(25.dp))
                .background(MaterialTheme.colorScheme.surface).border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(0.15f),
                    shape = RoundedCornerShape(25.dp)
                ).padding(horizontal = 15.dp)
        ) {
            item {
                Spacer(Modifier.height(10.dp))
            }
            item {
                AnimatedVisibility(!allTags.isRetrieving && isTagsEmpty) {
                    Text(
                        text = Localization.Key.NoTagsFound.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 75.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (!allTags.isRetrieving && isTagsEmpty) {
                return@LazyColumn
            }
            allTags.data.forEach { (pageKey, tags) ->
                items(items = tags, key = {
                    "TagSelectionComponent_P$pageKey-${it.localId}"
                }) {
                    val isTagSelected by rememberSaveable(selectedTags.contains(it)) {
                        mutableStateOf(selectedTags.contains(it))
                    }
                    AssistChip(
                        shape = RoundedCornerShape(15.dp),
                        colors = AssistChipDefaults.assistChipColors(containerColor = if (isTagSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isTagSelected) MaterialTheme.colorScheme.secondaryContainer else LocalContentColor.current.copy(
                                0.25f
                            )
                        ),
                        onClick = {
                            onTagClick(it)
                        },
                        label = {
                            Text(
                                text = it.name,
                                color = if (isTagSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isTagSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                tint = if (isTagSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary,
                                imageVector = Icons.Default.Tag,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.padding(top = 7.5.dp, bottom = 7.5.dp).fillMaxWidth()
                            .height(50.dp).pointerHoverIcon(icon = PointerIcon.Hand)
                    )
                }
            }
            if (!allTags.pagesCompleted) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}