package com.dabber.traveldabble.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared scroll state holder for bottom bar visibility with smooth hysteresis.
 * Eliminates jitter and flickering during minor scroll fluctuations.
 */
object ScrollState {
    var isBarVisible by mutableStateOf(true)
        private set

    private var lastScrollOffset = 0
    private var lastScrollIndex = 0
    private const val SCROLL_DOWN_THRESHOLD = 35
    private const val SCROLL_UP_THRESHOLD = 20

    fun onScroll(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        // At the top of the list, always show the bar
        if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset < 60) {
            if (!isBarVisible) isBarVisible = true
            lastScrollIndex = firstVisibleItemIndex
            lastScrollOffset = firstVisibleItemScrollOffset
            return
        }

        if (firstVisibleItemIndex != lastScrollIndex) {
            if (firstVisibleItemIndex > lastScrollIndex) {
                isBarVisible = false
            } else {
                isBarVisible = true
            }
            lastScrollIndex = firstVisibleItemIndex
            lastScrollOffset = firstVisibleItemScrollOffset
        } else {
            val delta = firstVisibleItemScrollOffset - lastScrollOffset
            if (delta > SCROLL_DOWN_THRESHOLD) {
                if (isBarVisible) isBarVisible = false
                lastScrollOffset = firstVisibleItemScrollOffset
            } else if (delta < -SCROLL_UP_THRESHOLD) {
                if (!isBarVisible) isBarVisible = true
                lastScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }

    fun reset() {
        isBarVisible = true
        lastScrollOffset = 0
        lastScrollIndex = 0
    }
}
