package com.anongram.web.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Коллекция иконок Microsoft FluentUI System Icons в формате Jetpack Compose ImageVector.
 * Включает иконку ChatMultiple и сопутствующие привлекательные системные иконки.
 */
object FluentIcons {

    /**
     * FluentUI ChatMultiple (два облачка чата)
     */
    val ChatMultiple: ImageVector
        get() {
            if (_chatMultiple != null) return _chatMultiple!!
            _chatMultiple = ImageVector.Builder(
                name = "chat-multiple",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(9.56158f, 3f)
                    curveTo(5.41944f, 3f, 2.06158f, 6.35786f, 2.06158f, 10.5f)
                    curveTo(2.06158f, 11.6329f, 2.31325f, 12.7088f, 2.76423f, 13.6734f)
                    curveTo(2.5102f, 14.6714f, 2.22638f, 15.7842f, 2.03999f, 16.5147f)
                    curveTo(1.80697f, 17.428f, 2.6294f, 18.2588f, 3.54374f, 18.039f)
                    curveTo(4.29396f, 17.8587f, 5.44699f, 17.5819f, 6.47447f, 17.337f)
                    curveTo(7.41678f, 17.7631f, 8.46241f, 18f, 9.56158f, 18f)
                    curveTo(13.7037f, 18f, 17.0616f, 14.6421f, 17.0616f, 10.5f)
                    curveTo(17.0616f, 6.35786f, 13.7037f, 3f, 9.56158f, 3f)
                    close()
                    moveTo(3.56158f, 10.5f)
                    curveTo(3.56158f, 7.18629f, 6.24787f, 4.5f, 9.56158f, 4.5f)
                    curveTo(12.8753f, 4.5f, 15.5616f, 7.18629f, 15.5616f, 10.5f)
                    curveTo(15.5616f, 13.8137f, 12.8753f, 16.5f, 9.56158f, 16.5f)
                    curveTo(8.60084f, 16.5f, 7.69487f, 16.2748f, 6.89161f, 15.8749f)
                    lineTo(6.6482f, 15.7537f)
                    lineTo(6.38368f, 15.8167f)
                    curveTo(5.46095f, 16.0363f, 4.39489f, 16.2919f, 3.59592f, 16.4838f)
                    curveTo(3.79467f, 15.7047f, 4.05784f, 14.6724f, 4.28601f, 13.7757f)
                    lineTo(4.35619f, 13.4998f)
                    lineTo(4.22568f, 13.2468f)
                    curveTo(3.80145f, 12.4246f, 3.56158f, 11.4914f, 3.56158f, 10.5f)
                    close()
                    moveTo(14.5616f, 21.0001f)
                    curveTo(12.5922f, 21.0001f, 10.8001f, 20.241f, 9.46191f, 18.9995f)
                    curveTo(9.49511f, 18.9999f, 9.52835f, 19.0001f, 9.56163f, 19.0001f)
                    curveTo(10.2796f, 19.0001f, 10.9768f, 18.911f, 11.6427f, 18.7434f)
                    curveTo(12.5067f, 19.2254f, 13.5021f, 19.5001f, 14.5616f, 19.5001f)
                    curveTo(15.5223f, 19.5001f, 16.4283f, 19.2748f, 17.2316f, 18.8749f)
                    lineTo(17.475f, 18.7537f)
                    lineTo(17.7395f, 18.8167f)
                    curveTo(18.6611f, 19.0361f, 19.7046f, 19.2625f, 20.4787f, 19.4262f)
                    curveTo(20.3037f, 18.6757f, 20.065f, 17.6711f, 19.8372f, 16.7757f)
                    lineTo(19.767f, 16.4999f)
                    lineTo(19.8975f, 16.2469f)
                    curveTo(20.3217f, 15.4247f, 20.5616f, 14.4915f, 20.5616f, 13.5001f)
                    curveTo(20.5616f, 11.3853f, 19.4676f, 9.52617f, 17.8146f, 8.45761f)
                    curveTo(17.6363f, 7.73435f, 17.3653f, 7.04756f, 17.015f, 6.41052f)
                    curveTo(19.9523f, 7.42684f, 22.0616f, 10.2171f, 22.0616f, 13.5001f)
                    curveTo(22.0616f, 14.6332f, 21.8098f, 15.7094f, 21.3586f, 16.6741f)
                    curveTo(21.6117f, 17.6821f, 21.8679f, 18.774f, 22.0304f, 19.4773f)
                    curveTo(22.2348f, 20.3623f, 21.4554f, 21.1633f, 20.563f, 20.9768f)
                    curveTo(19.8358f, 20.8248f, 18.6933f, 20.581f, 17.6495f, 20.3367f)
                    curveTo(16.707f, 20.763f, 15.6611f, 21.0001f, 14.5616f, 21.0001f)
                    close()
                }
            }.build()
            return _chatMultiple!!
        }
    private var _chatMultiple: ImageVector? = null

    /**
     * FluentUI Shield (Щит / Сервисы / Безопасность)
     */
    val Shield: ImageVector
        get() {
            if (_shield != null) return _shield!!
            _shield = ImageVector.Builder(
                name = "shield",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 2f)
                    curveTo(7.5f, 3.5f, 4f, 3.8f, 4f, 4f)
                    verticalLineTo(11f)
                    curveTo(4f, 16.5f, 7.5f, 21f, 12f, 22f)
                    curveTo(16.5f, 21f, 20f, 16.5f, 20f, 11f)
                    verticalLineTo(4f)
                    curveTo(20f, 3.8f, 16.5f, 3.5f, 12f, 2f)
                    close()
                    moveTo(12f, 3.8f)
                    curveTo(15.2f, 5.1f, 18.2f, 5.4f, 18.2f, 5.5f)
                    verticalLineTo(11f)
                    curveTo(18.2f, 15.5f, 15.4f, 19.3f, 12f, 20.2f)
                    curveTo(8.6f, 19.3f, 5.8f, 15.5f, 5.8f, 11f)
                    verticalLineTo(5.5f)
                    curveTo(5.8f, 5.4f, 8.8f, 5.1f, 12f, 3.8f)
                    close()
                }
            }.build()
            return _shield!!
        }
    private var _shield: ImageVector? = null

    /**
     * FluentUI Settings (Шестерёнка настроек)
     */
    val Settings: ImageVector
        get() {
            if (_settings != null) return _settings!!
            _settings = ImageVector.Builder(
                name = "settings",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 8.5f)
                    curveTo(10.07f, 8.5f, 8.5f, 10.07f, 8.5f, 12f)
                    curveTo(8.5f, 13.93f, 10.07f, 15.5f, 12f, 15.5f)
                    curveTo(13.93f, 15.5f, 15.5f, 13.93f, 15.5f, 12f)
                    curveTo(15.5f, 10.07f, 13.93f, 8.5f, 12f, 8.5f)
                    close()
                    moveTo(12f, 13.75f)
                    curveTo(11.03f, 13.75f, 10.25f, 12.97f, 10.25f, 12f)
                    curveTo(10.25f, 11.03f, 11.03f, 10.25f, 12f, 10.25f)
                    curveTo(12.97f, 10.25f, 13.75f, 11.03f, 13.75f, 12f)
                    curveTo(13.75f, 12.97f, 12.97f, 13.75f, 12f, 13.75f)
                    close()
                    moveTo(19.9f, 12.7f)
                    lineTo(18.5f, 12.5f)
                    curveTo(18.4f, 12.1f, 18.2f, 11.7f, 18.0f, 11.3f)
                    lineTo(18.9f, 10.2f)
                    curveTo(19.2f, 9.8f, 19.1f, 9.3f, 18.7f, 9.0f)
                    lineTo(17.3f, 7.6f)
                    curveTo(17.0f, 7.3f, 16.5f, 7.2f, 16.1f, 7.5f)
                    lineTo(15.0f, 8.4f)
                    curveTo(14.6f, 8.2f, 14.2f, 8.0f, 13.8f, 7.9f)
                    lineTo(13.6f, 6.5f)
                    curveTo(13.5f, 6.0f, 13.1f, 5.7f, 12.6f, 5.7f)
                    horizontalLineTo(10.6f)
                    curveTo(10.1f, 5.7f, 9.7f, 6.0f, 9.6f, 6.5f)
                    lineTo(9.4f, 7.9f)
                    curveTo(9.0f, 8.0f, 8.6f, 8.2f, 8.2f, 8.4f)
                    lineTo(7.1f, 7.5f)
                    curveTo(6.7f, 7.2f, 6.2f, 7.3f, 5.9f, 7.6f)
                    lineTo(4.5f, 9.0f)
                    curveTo(4.2f, 9.3f, 4.1f, 9.8f, 4.4f, 10.2f)
                    lineTo(5.3f, 11.3f)
                    curveTo(5.1f, 11.7f, 4.9f, 12.1f, 4.8f, 12.5f)
                    lineTo(3.4f, 12.7f)
                    curveTo(2.9f, 12.8f, 2.6f, 13.2f, 2.6f, 13.7f)
                    verticalLineTo(15.7f)
                    curveTo(2.6f, 16.2f, 2.9f, 16.6f, 3.4f, 16.7f)
                    lineTo(4.8f, 16.9f)
                    curveTo(4.9f, 17.3f, 5.1f, 17.7f, 5.3f, 18.1f)
                    lineTo(4.4f, 19.2f)
                    curveTo(4.1f, 19.6f, 4.2f, 20.1f, 4.5f, 20.4f)
                    lineTo(5.9f, 21.8f)
                    curveTo(6.2f, 22.1f, 6.7f, 22.2f, 7.1f, 21.9f)
                    lineTo(8.2f, 21.0f)
                    curveTo(8.6f, 21.2f, 9.0f, 21.4f, 9.4f, 21.5f)
                    lineTo(9.6f, 22.9f)
                    curveTo(9.7f, 23.4f, 10.1f, 23.7f, 10.6f, 23.7f)
                    horizontalLineTo(12.6f)
                    curveTo(13.1f, 23.7f, 13.5f, 23.4f, 13.6f, 22.9f)
                    lineTo(13.8f, 21.5f)
                    curveTo(14.2f, 21.4f, 14.6f, 21.2f, 15.0f, 21.0f)
                    lineTo(16.1f, 21.9f)
                    curveTo(16.5f, 22.2f, 17.0f, 22.1f, 17.3f, 21.8f)
                    lineTo(18.7f, 20.4f)
                    curveTo(19.1f, 20.1f, 19.2f, 19.6f, 18.9f, 19.2f)
                    lineTo(18.0f, 18.1f)
                    curveTo(18.2f, 17.7f, 18.4f, 17.3f, 18.5f, 16.9f)
                    lineTo(19.9f, 16.7f)
                    curveTo(20.4f, 16.6f, 20.7f, 16.2f, 20.7f, 15.7f)
                    verticalLineTo(13.7f)
                    curveTo(20.7f, 13.2f, 20.4f, 12.8f, 19.9f, 12.7f)
                    close()
                }
            }.build()
            return _settings!!
        }
    private var _settings: ImageVector? = null

    /**
     * FluentUI Person (Профиль)
     */
    val Person: ImageVector
        get() {
            if (_person != null) return _person!!
            _person = ImageVector.Builder(
                name = "person",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 12f)
                    curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
                    curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
                    curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
                    curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
                    close()
                    moveTo(12f, 5.75f)
                    curveTo(13.24f, 5.75f, 14.25f, 6.76f, 14.25f, 8f)
                    curveTo(14.25f, 9.24f, 13.24f, 10.25f, 12f, 10.25f)
                    curveTo(10.76f, 10.25f, 9.75f, 9.24f, 9.75f, 8f)
                    curveTo(9.75f, 6.76f, 10.76f, 5.75f, 12f, 5.75f)
                    close()
                    moveTo(4.5f, 19.5f)
                    curveTo(4.5f, 16.5f, 7.8f, 14f, 12f, 14f)
                    curveTo(16.2f, 14f, 19.5f, 16.5f, 19.5f, 19.5f)
                    curveTo(19.5f, 20.05f, 19.05f, 20.5f, 18.5f, 20.5f)
                    horizontalLineTo(5.5f)
                    curveTo(4.95f, 20.5f, 4.5f, 20.05f, 4.5f, 19.5f)
                    close()
                    moveTo(6.3f, 18.75f)
                    horizontalLineTo(17.7f)
                    curveTo(17.2f, 16.8f, 14.9f, 15.75f, 12f, 15.75f)
                    curveTo(9.1f, 15.75f, 6.8f, 16.8f, 6.3f, 18.75f)
                    close()
                }
            }.build()
            return _person!!
        }
    private var _person: ImageVector? = null

    /**
     * FluentUI Sparkle (Звёздочка / Анимации)
     */
    val Sparkle: ImageVector
        get() {
            if (_sparkle != null) return _sparkle!!
            _sparkle = ImageVector.Builder(
                name = "sparkle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 2f)
                    curveTo(12f, 7.5f, 7.5f, 12f, 2f, 12f)
                    curveTo(7.5f, 12f, 12f, 16.5f, 12f, 22f)
                    curveTo(12f, 16.5f, 16.5f, 12f, 22f, 12f)
                    curveTo(16.5f, 12f, 12f, 7.5f, 12f, 2f)
                    close()
                }
            }.build()
            return _sparkle!!
        }
    private var _sparkle: ImageVector? = null
}
