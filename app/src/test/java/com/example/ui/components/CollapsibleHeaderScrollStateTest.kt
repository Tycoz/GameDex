package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsibleHeaderScrollStateTest {

    @Test
    fun scrollDownHidesHeader_andScrollUpRestoresHeader() {
        var now = 1000L
        val state = CollapsibleHeaderScrollState(
            initialVisible = true,
            thresholdPx = 24f,
            timeProvider = { now }
        )

        assertTrue(state.isHeaderVisible)

        // Small jitter below threshold keeps header visible
        state.onVerticalDelta(-10f)
        assertTrue(state.isHeaderVisible)

        // Reaching threshold hides header rows
        state.onVerticalDelta(-20f)
        assertFalse(state.isHeaderVisible)

        // Small upward jitter stays hidden
        state.onVerticalDelta(10f)
        assertFalse(state.isHeaderVisible)

        // Scrolling up beyond threshold reveals all header rows again
        state.onVerticalDelta(20f)
        assertTrue(state.isHeaderVisible)
    }

    @Test
    fun nestedScrollConnection_ignoresHorizontalDominantDrags() {
        val state = CollapsibleHeaderScrollState(thresholdPx = 24f)

        // Horizontal chip-bar style drag should not collapse header even if y has tiny component
        state.nestedScrollConnection.onPreScroll(
            available = Offset(x = -60f, y = -30f),
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.isHeaderVisible)

        // Vertical dominant downward scroll collapses header
        state.nestedScrollConnection.onPreScroll(
            available = Offset(x = -5f, y = -30f),
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.isHeaderVisible)

        // Vertical dominant upward scroll restores header
        state.nestedScrollConnection.onPreScroll(
            available = Offset(x = 2f, y = 30f),
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.isHeaderVisible)
    }

    @Test
    fun layoutClampCooldown_preventsBounceImmediatelyAfterToggle() {
        var now = 1000L
        val state = CollapsibleHeaderScrollState(
            initialVisible = true,
            thresholdPx = 24f,
            timeProvider = { now }
        )

        // User scrolls down -> hides header at t=1000
        state.onVerticalDelta(-30f)
        assertFalse(state.isHeaderVisible)

        // Layout clamp reduces offset by 40px at t=1100 (within 300ms cooldown) -> ignored
        now = 1100L
        state.onListOffsetChanged(prevIndex = 3, prevOffset = 100, newIndex = 3, newOffset = 60)
        assertFalse(state.isHeaderVisible)

        // After cooldown (t=1400), scrolling up restores header
        now = 1400L
        state.onListOffsetChanged(prevIndex = 3, prevOffset = 60, newIndex = 2, newOffset = 10)
        assertTrue(state.isHeaderVisible)
    }
}
