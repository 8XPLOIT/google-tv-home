package io.github.samuelfonseca.tvhome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A 720px window; the dock reaches the top at 500 and the page ends at 1400. */
class ScrollTest {
    private static final int VIEWPORT = 720, MIN = 500, MAX = 1400;

    @Test void firstRowFromTheDockPutsTheDockAtTheTop() {
        assertEquals(MIN, Scroll.toShow(0, 900, 1060, VIEWPORT, MIN, MAX));
    }

    @Test void visibleRowDoesNotScroll() {
        assertEquals(600, Scroll.toShow(600, 900, 1060, VIEWPORT, MIN, MAX));
    }

    @Test void rowBelowScrollsDownJustEnough() {
        assertEquals(1300 - VIEWPORT, Scroll.toShow(MIN, 1140, 1300, VIEWPORT, MIN, MAX));
    }

    @Test void rowAboveScrollsUpToItsTop() {
        assertEquals(700, Scroll.toShow(900, 700, 860, VIEWPORT, MIN, MAX));
    }

    @Test void neverAboveTheDockPosition() {
        assertEquals(MIN, Scroll.toShow(900, 300, 460, VIEWPORT, MIN, MAX));
    }

    @Test void neverPastTheEndOfThePage() {
        assertEquals(MAX, Scroll.toShow(MIN, 2000, 2200, VIEWPORT, MIN, MAX));
    }
}
