package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.highlightOnFocused

@Stable
data class AddItemFABParam(
    val onCreateATagClick: () -> Unit,
    val isReducedTransparencyBoxVisible: Boolean,
    val onShowDialogForNewFolder: () -> Unit,
    val onShowAddLinkDialog: () -> Unit,
    val isMainFabRotated: Boolean,
    val rotationAnimatable: Animatable<Float, AnimationVector1D>,
    val inASpecificScreen: Boolean,
    val hideReducedTransparencyBox: () -> Unit,
    val showReducedTransparencyBox: () -> Unit,
    val undoMainFabRotation: () -> Unit,
    val rotateMainFab: () -> Unit,
)

@Composable
fun AddItemFab(addItemFABParam: AddItemFABParam) {
    val localizedStrings = LocalizedStrings.current
    val currentIconForMainFAB =
        if (addItemFABParam.isMainFabRotated) {
            Icons.Default.AddLink
        } else {
            Icons.Default.Add
        }

    var hasInitializedRotation by remember { mutableStateOf(false) }

    LaunchedEffect(addItemFABParam.isMainFabRotated) {
        val targetRotation =
            if (addItemFABParam.isMainFabRotated) {
                180f
            } else {
                0f
            }

        if (!hasInitializedRotation) {
            hasInitializedRotation = true
            addItemFABParam.rotationAnimatable.snapTo(targetRotation)
            return@LaunchedEffect
        }

        addItemFABParam.rotationAnimatable.animateTo(
            targetValue = targetRotation,
            animationSpec =
                tween(
                    durationMillis = 450,
                    easing = FastOutSlowInEasing,
                ),
        )
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .align(Alignment.End)
                    .focusGroup()
                    .highlightOnFocused(shape = RoundedCornerShape(15.dp))
                    .padding(top = 7.5.dp, bottom = 7.5.dp, start = 7.5.dp, end = 7.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = addItemFABParam.isMainFabRotated,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Text(
                    text = localizedStrings.CreateANewTag,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 15.dp),
                )
            }
            AnimatedVisibility(
                visible = addItemFABParam.isMainFabRotated,
                enter = androidx.compose.animation.scaleIn(animationSpec = tween(300)),
                exit = androidx.compose.animation.scaleOut(tween(300)),
            ) {
                FloatingActionButton(
                    modifier =
                        Modifier
                            .pressScaleEffect()
                            .pointerHoverIcon(icon = PointerIcon.Hand),
                    onClick = {
                        addItemFABParam.hideReducedTransparencyBox()
                        addItemFABParam.onCreateATagClick()
                        addItemFABParam.undoMainFabRotation()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .align(Alignment.End)
                    .focusGroup()
                    .highlightOnFocused(shape = RoundedCornerShape(15.dp))
                    .padding(top = 7.5.dp, bottom = 7.5.dp, start = 7.5.dp, end = 7.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = addItemFABParam.isMainFabRotated,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Text(
                    text = localizedStrings.CreateANewFolder,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 15.dp),
                )
            }

            AnimatedVisibility(
                visible = addItemFABParam.isMainFabRotated,
                enter = androidx.compose.animation.scaleIn(animationSpec = tween(300)),
                exit = androidx.compose.animation.scaleOut(tween(300)),
            ) {
                FloatingActionButton(
                    modifier =
                        Modifier
                            .pressScaleEffect()
                            .pointerHoverIcon(icon = PointerIcon.Hand),
                    onClick = {
                        addItemFABParam.hideReducedTransparencyBox()
                        addItemFABParam.onShowDialogForNewFolder()
                        addItemFABParam.undoMainFabRotation()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .align(Alignment.End)
                    .focusGroup()
                    .highlightOnFocused(shape = RoundedCornerShape(15.dp))
                    .padding(top = 7.5.dp, bottom = 7.5.dp, start = 7.5.dp, end = 7.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = addItemFABParam.isMainFabRotated,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
            ) {
                Text(
                    text = localizedStrings.AddANewLink,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 15.dp),
                )
            }

            FloatingActionButton(
                modifier =
                    Modifier
                        .graphicsLayer {
                            rotationZ = addItemFABParam.rotationAnimatable.value
                        }
                        .pressScaleEffect()
                        .pointerHoverIcon(icon = PointerIcon.Hand),
                onClick = {
                    if (addItemFABParam.isMainFabRotated) {
                        addItemFABParam.hideReducedTransparencyBox()
                        addItemFABParam.onShowAddLinkDialog()
                        addItemFABParam.undoMainFabRotation()
                    } else {
                        addItemFABParam.showReducedTransparencyBox()
                        addItemFABParam.rotateMainFab()
                    }
                },
            ) {
                AnimatedContent(
                    targetState = currentIconForMainFAB,
                    transitionSpec = {
                        fadeIn(tween(500)) togetherWith fadeOut(tween(250))
                    },
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
