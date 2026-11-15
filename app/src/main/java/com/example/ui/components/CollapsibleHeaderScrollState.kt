package com.example.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * Tracks vertical scroll direction for lists with a collapsible header below a pinned search bar.
 * Scrolling down collapses secondary header rows; scrolling up brings all header rows back.
 */
@Stable
class CollapsibleHeaderScrollState(
    initialVisible: Boolean = true,
    private val thresholdPx: Float = 24f,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    var isHeaderVisible by mutableStateOf(initialVisible)
        private set

    private var scrollAccumulator = 0f
    private var lastToggleTimeMs = 0L

    fun showHeader() {
        if (!isHeaderVisible) {
            isHeaderVisible = true
            lastToggleTimeMs = timeProvider()
        }
        scrollAccumulator = 0f
    }

    fun hideHeader() {
        if (isHeaderVisible) {
            isHeaderVisible = false
            lastToggleTimeMs = timeProvider()
        }
        scrollAccumulator = 0f
    }

    /**
     * Applies a vertical scroll delta in NestedScroll coordinates:
     * dy < 0 -> scrolling down (content moves up, finger swipes up) -> hide secondary rows.
     * dy > 0 -> scrolling up (content moves down, finger swipes down) -> show secondary rows.
     */
    fun onVerticalDelta(dy: Float) {
        if (dy == 0f) return
        if (dy < 0f) {
            if (scrollAccumulator > 0f) scrollAccumulator = 0f
            scrollAccumulator += dy
            if (scrollAccumulator <= -thresholdPx) {
                hideHeader()
            }
        } else {
            if (scrollAccumulator < 0f) scrollAccumulator = 0f
            scrollAccumulator += dy
            if (scrollAccumulator >= thresholdPx) {
                showHeader()
            }
        }
    }

    internal fun onListOffsetChanged(
        prevIndex: Int,
        prevOffset: Int,
        newIndex: Int,
        newOffset: Int
    ) {
        val now = timeProvider()
        // Prevent layout-induced offset adjustments right after AnimatedVisibility resizes the header
        if (now - lastToggleTimeMs < 300L) return

        val deltaIndex = newIndex - prevIndex
        val deltaOffset = newOffset - prevOffset

        if (deltaIndex > 0 || (deltaIndex == 0 && deltaOffset > thresholdPx.toInt())) {
            hideHeader()
        } else if (deltaIndex < 0 || (deltaIndex == 0 && deltaOffset < -thresholdPx.toInt())) {
            showHeader()
        } else if (newIndex == 0 && newOffset == 0 && (prevIndex > 0 || prevOffset > 0)) {
            showHeader()
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (abs(available.y) > 0f && abs(available.y) >= abs(available.x)) {
                onVerticalDelta(available.y)
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberCollapsibleHeaderScrollState(
    listState: LazyListState,
    thresholdPx: Float = 24f
): CollapsibleHeaderScrollState {
    val state = remember(thresholdPx) {
        CollapsibleHeaderScrollState(thresholdPx = thresholdPx)
    }

    LaunchedEffect(listState, state) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (newIndex, newOffset) ->
            state.onListOffsetChanged(prevIndex, prevOffset, newIndex, newOffset)
            prevIndex = newIndex
            prevOffset = newOffset
        }
    }

    return state
}
