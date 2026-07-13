package com.sakethh.linkora.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

enum class OnClickState {
    IDLE,
    GESTURED,
}

fun Modifier.pressScaleEffect(targetValue: Float = 0.98f) = composed {
    val composableState = remember {
        mutableStateOf(OnClickState.IDLE)
    }
    val scaleValue =
        animateFloatAsState(
            label = "",
            targetValue = if (composableState.value == OnClickState.IDLE) 1.0f else targetValue,
        )
    this.graphicsLayer {
        scaleX = scaleValue.value
        scaleY = scaleValue.value
    }
        .pointerInput(composableState.value) {
            awaitPointerEventScope {
                composableState.value =
                    if (composableState.value == OnClickState.IDLE) {
                        awaitFirstDown(false)
                        OnClickState.GESTURED
                    } else {
                        waitForUpOrCancellation()
                        OnClickState.IDLE
                    }
            }
        }
}
