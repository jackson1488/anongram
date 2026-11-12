package com.anongram.app.features.chats.attach.state

import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.anongram.app.features.player.logic.AudioPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
internal class AttachSheetDragState(
    val totalHeightPx: Float,
    private val scope: CoroutineScope,
    private val context: Context,
    private val wasKeyboardOpen: Boolean,
    private val onRestoreKeyboard: () -> Unit,
    private val getOnDismiss: () -> () -> Unit,
    private val getTotalSelectedCount: () -> Int,
    private val onShowDiscardDialog: () -> Unit,
    private val onCollapseAlbumsSheet: () -> Unit
) {
    val halfOffset: Float = totalHeightPx * 0.5f
    val openOffset: Float = totalHeightPx * 0.45f

    var offsetPx by mutableFloatStateOf(totalHeightPx)
    var isClosing by mutableStateOf(false)
    var animJob: Job? = null

    val isFull by derivedStateOf {
        offsetPx <= 20f || (totalHeightPx > 0f && offsetPx <= totalHeightPx * 0.05f)
    }

    val isExpanded95 by derivedStateOf {
        totalHeightPx > 0f && offsetPx <= totalHeightPx * 0.05f
    }

    val isLocationHeaderVisible by derivedStateOf {
        totalHeightPx > 0f && offsetPx <= totalHeightPx * 0.20f
    }

    private val decelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    private val smoothInOutEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    fun animateTo(target: Float, ms: Int = 260, isFling: Boolean = false, end: () -> Unit = {}) {
        animJob?.cancel()
        val easing = if (isFling) decelerateEasing else smoothInOutEasing
        animJob = scope.launch {
            animate(
                initialValue = offsetPx,
                targetValue = target,
                animationSpec = tween(ms, easing = easing)
            ) { v, _ -> offsetPx = v }
            end()
        }
    }

    fun forceDismiss() {
        if (isClosing) return
        isClosing = true
        try {
            AudioPlayerController.getInstance(context).stop()
        } catch (_: Exception) {}
        if (wasKeyboardOpen) {
            onRestoreKeyboard()
        }
        animateTo(totalHeightPx, 240, isFling = true) { getOnDismiss().invoke() }
    }

    fun close() {
        if (getTotalSelectedCount() > 0) {
            onShowDiscardDialog()
            animateTo(halfOffset, 200, isFling = false)
            return
        }
        forceDismiss()
    }

    fun dismissAnimated() {
        if (isClosing) return
        if (getTotalSelectedCount() > 0) {
            onShowDiscardDialog()
            animateTo(halfOffset, 200, isFling = false)
            return
        }
        forceDismiss()
    }

    fun release(velocityY: Float) {
        animJob?.cancel()
        if (offsetPx <= halfOffset) {
            val shouldExpand = velocityY < -600f || (velocityY <= 600f && offsetPx < halfOffset * 0.55f)
            val target = if (shouldExpand) 0f else halfOffset
            animateTo(target, ms = 220, isFling = kotlin.math.abs(velocityY) > 600f)
            return
        }
        if (velocityY > 2000f || offsetPx > halfOffset + 80f) {
            if (getTotalSelectedCount() > 0) {
                onShowDiscardDialog()
                animateTo(halfOffset, ms = 200, isFling = true)
            } else {
                close()
            }
        } else {
            animateTo(halfOffset, ms = 200, isFling = false)
        }
    }

    fun requestDismiss() {
        if (getTotalSelectedCount() > 0) {
            onShowDiscardDialog()
        } else {
            dismissAnimated()
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            onCollapseAlbumsSheet()
            val dy = available.y
            if (dy < 0f && offsetPx > 0f) {
                animJob?.cancel()
                val new = (offsetPx + dy).coerceAtLeast(0f)
                val used = new - offsetPx
                offsetPx = new
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            onCollapseAlbumsSheet()
            val dy = available.y
            if (dy > 0f) {
                animJob?.cancel()
                val maxOffset = if (getTotalSelectedCount() > 0) halfOffset + 120f else totalHeightPx
                val new = (offsetPx + dy).coerceAtMost(maxOffset)
                val used = new - offsetPx
                offsetPx = new
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offsetPx > 0f && offsetPx < totalHeightPx && available.y < -600f) {
                release(available.y)
                return Velocity(0f, available.y)
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (offsetPx > 0f && offsetPx < totalHeightPx) {
                release(available.y)
            }
            return Velocity.Zero
        }
    }
}

@Composable
internal fun rememberAttachSheetDragState(
    totalHeightPx: Float,
    context: Context,
    wasKeyboardOpen: Boolean,
    onRestoreKeyboard: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: AttachSheetState
): AttachSheetDragState {
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    return remember(totalHeightPx) {
        AttachSheetDragState(
            totalHeightPx = totalHeightPx,
            scope = scope,
            context = context,
            wasKeyboardOpen = wasKeyboardOpen,
            onRestoreKeyboard = onRestoreKeyboard,
            getOnDismiss = { currentOnDismiss },
            getTotalSelectedCount = { sheetState.totalSelectedCount },
            onShowDiscardDialog = { sheetState.showDiscardDialog = true },
            onCollapseAlbumsSheet = {
                if (sheetState.isAlbumsSheetOpen) sheetState.isAlbumsSheetOpen = false
            }
        )
    }
}
