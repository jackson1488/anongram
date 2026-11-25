package com.anongram.app.features.chats.mediaeditor.model

enum class CurveChannel {
    ALL,
    RED,
    GREEN,
    BLUE
}

data class CurvePoint(
    val x: Float,
    val y: Float
)

data class ToneCurvesState(
    val activeChannel: CurveChannel = CurveChannel.ALL,
    val allPoints: List<CurvePoint> = defaultPoints,
    val redPoints: List<CurvePoint> = defaultPoints,
    val greenPoints: List<CurvePoint> = defaultPoints,
    val bluePoints: List<CurvePoint> = defaultPoints
) {
    fun pointsFor(channel: CurveChannel): List<CurvePoint> = when (channel) {
        CurveChannel.ALL -> allPoints
        CurveChannel.RED -> redPoints
        CurveChannel.GREEN -> greenPoints
        CurveChannel.BLUE -> bluePoints
    }

    fun withPoints(channel: CurveChannel, points: List<CurvePoint>): ToneCurvesState {
        val sorted = points.sortedBy { it.x }
        return when (channel) {
            CurveChannel.ALL -> copy(allPoints = sorted)
            CurveChannel.RED -> copy(redPoints = sorted)
            CurveChannel.GREEN -> copy(greenPoints = sorted)
            CurveChannel.BLUE -> copy(bluePoints = sorted)
        }
    }

    val isModified: Boolean
        get() = allPoints != defaultPoints ||
            redPoints != defaultPoints ||
            greenPoints != defaultPoints ||
            bluePoints != defaultPoints

    companion object {
        val defaultPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
    }
}

enum class BlurMode {
    OFF,
    RADIAL,
    LINEAR
}

data class BlurEffectState(
    val mode: BlurMode = BlurMode.OFF,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val radius: Float = 0.30f,
    val angleDegrees: Float = 0f,
    val intensity: Float = 0.65f
) {
    val isModified: Boolean get() = mode != BlurMode.OFF
}
