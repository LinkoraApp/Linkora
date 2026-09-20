package com.sakethh.linkora.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Icons {

    val Info: ImageVector
        get() {
            if (_Info != null) {
                return _Info!!
            }
            _Info = ImageVector.Builder(
                name = "Info",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(440f, 680f)
                    horizontalLineToRelative(80f)
                    verticalLineToRelative(-240f)
                    horizontalLineToRelative(-80f)
                    verticalLineToRelative(240f)
                    close()
                    moveTo(508.5f, 348.5f)
                    quadTo(520f, 337f, 520f, 320f)
                    reflectiveQuadToRelative(-11.5f, -28.5f)
                    quadTo(497f, 280f, 480f, 280f)
                    reflectiveQuadToRelative(-28.5f, 11.5f)
                    quadTo(440f, 303f, 440f, 320f)
                    reflectiveQuadToRelative(11.5f, 28.5f)
                    quadTo(463f, 360f, 480f, 360f)
                    reflectiveQuadToRelative(28.5f, -11.5f)
                    close()
                    moveTo(480f, 880f)
                    quadToRelative(-83f, 0f, -156f, -31.5f)
                    reflectiveQuadTo(197f, 763f)
                    quadToRelative(-54f, -54f, -85.5f, -127f)
                    reflectiveQuadTo(80f, 480f)
                    quadToRelative(0f, -83f, 31.5f, -156f)
                    reflectiveQuadTo(197f, 197f)
                    quadToRelative(54f, -54f, 127f, -85.5f)
                    reflectiveQuadTo(480f, 80f)
                    quadToRelative(83f, 0f, 156f, 31.5f)
                    reflectiveQuadTo(763f, 197f)
                    quadToRelative(54f, 54f, 85.5f, 127f)
                    reflectiveQuadTo(880f, 480f)
                    quadToRelative(0f, 83f, -31.5f, 156f)
                    reflectiveQuadTo(763f, 763f)
                    quadToRelative(-54f, 54f, -127f, 85.5f)
                    reflectiveQuadTo(480f, 880f)
                    close()
                    moveTo(480f, 800f)
                    quadToRelative(134f, 0f, 227f, -93f)
                    reflectiveQuadToRelative(93f, -227f)
                    quadToRelative(0f, -134f, -93f, -227f)
                    reflectiveQuadToRelative(-227f, -93f)
                    quadToRelative(-134f, 0f, -227f, 93f)
                    reflectiveQuadToRelative(-93f, 227f)
                    quadToRelative(0f, 134f, 93f, 227f)
                    reflectiveQuadToRelative(227f, 93f)
                    close()
                    moveTo(480f, 480f)
                    close()
                }
            }.build()

            return _Info!!
        }

    @Suppress("ObjectPropertyName")
    private var _Info: ImageVector? = null

    val Minimize: ImageVector
        get() {
            if (_Minimize != null) {
                return _Minimize!!
            }
            _Minimize = ImageVector.Builder(
                name = "Minimize",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(240f, 840f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(480f)
                    verticalLineToRelative(80f)
                    lineTo(240f, 840f)
                    close()
                }
            }.build()

            return _Minimize!!
        }

    @Suppress("ObjectPropertyName")
    private var _Minimize: ImageVector? = null

    val Maximize: ImageVector
        get() {
            if (_Maximize != null) {
                return _Maximize!!
            }
            _Maximize = ImageVector.Builder(
                name = "Maximize",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(160f, 200f)
                    verticalLineToRelative(-80f)
                    horizontalLineToRelative(640f)
                    verticalLineToRelative(80f)
                    lineTo(160f, 200f)
                    close()
                }
            }.build()

            return _Maximize!!
        }

    @Suppress("ObjectPropertyName")
    private var _Maximize: ImageVector? = null

    val Window: ImageVector
        get() {
            if (_Window != null) {
                return _Window!!
            }
            _Window = ImageVector.Builder(
                name = "Window",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(200f, 840f)
                    quadToRelative(-33f, 0f, -56.5f, -23.5f)
                    reflectiveQuadTo(120f, 760f)
                    verticalLineToRelative(-560f)
                    quadToRelative(0f, -33f, 23.5f, -56.5f)
                    reflectiveQuadTo(200f, 120f)
                    horizontalLineToRelative(560f)
                    quadToRelative(33f, 0f, 56.5f, 23.5f)
                    reflectiveQuadTo(840f, 200f)
                    verticalLineToRelative(560f)
                    quadToRelative(0f, 33f, -23.5f, 56.5f)
                    reflectiveQuadTo(760f, 840f)
                    lineTo(200f, 840f)
                    close()
                    moveTo(520f, 520f)
                    verticalLineToRelative(240f)
                    horizontalLineToRelative(240f)
                    verticalLineToRelative(-240f)
                    lineTo(520f, 520f)
                    close()
                    moveTo(520f, 440f)
                    horizontalLineToRelative(240f)
                    verticalLineToRelative(-240f)
                    lineTo(520f, 200f)
                    verticalLineToRelative(240f)
                    close()
                    moveTo(440f, 440f)
                    verticalLineToRelative(-240f)
                    lineTo(200f, 200f)
                    verticalLineToRelative(240f)
                    horizontalLineToRelative(240f)
                    close()
                    moveTo(440f, 520f)
                    lineTo(200f, 520f)
                    verticalLineToRelative(240f)
                    horizontalLineToRelative(240f)
                    verticalLineToRelative(-240f)
                    close()
                }
            }.build()

            return _Window!!
        }

    @Suppress("ObjectPropertyName")
    private var _Window: ImageVector? = null

    val Close: ImageVector
        get() {
            if (_Close != null) {
                return _Close!!
            }
            _Close = ImageVector.Builder(
                name = "Close",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveToRelative(256f, 760f)
                    lineToRelative(-56f, -56f)
                    lineToRelative(224f, -224f)
                    lineToRelative(-224f, -224f)
                    lineToRelative(56f, -56f)
                    lineToRelative(224f, 224f)
                    lineToRelative(224f, -224f)
                    lineToRelative(56f, 56f)
                    lineToRelative(-224f, 224f)
                    lineToRelative(224f, 224f)
                    lineToRelative(-56f, 56f)
                    lineToRelative(-224f, -224f)
                    lineToRelative(-224f, 224f)
                    close()
                }
            }.build()

            return _Close!!
        }

    @Suppress("ObjectPropertyName")
    private var _Close: ImageVector? = null

    val WbCloudy: ImageVector
        get() {
            if (_WbCloudy != null) {
                return _WbCloudy!!
            }
            _WbCloudy = ImageVector.Builder(
                name = "WbCloudy",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(260f, 800f)
                    quadToRelative(-91f, 0f, -155.5f, -63f)
                    reflectiveQuadTo(40f, 583f)
                    quadToRelative(0f, -78f, 47f, -139f)
                    reflectiveQuadToRelative(123f, -78f)
                    quadToRelative(25f, -92f, 100f, -149f)
                    reflectiveQuadToRelative(170f, -57f)
                    quadToRelative(117f, 0f, 198.5f, 81.5f)
                    reflectiveQuadTo(760f, 440f)
                    quadToRelative(69f, 8f, 114.5f, 59.5f)
                    reflectiveQuadTo(920f, 620f)
                    quadToRelative(0f, 75f, -52.5f, 127.5f)
                    reflectiveQuadTo(740f, 800f)
                    lineTo(260f, 800f)
                    close()
                }
            }.build()

            return _WbCloudy!!
        }

    @Suppress("ObjectPropertyName")
    private var _WbCloudy: ImageVector? = null
}
