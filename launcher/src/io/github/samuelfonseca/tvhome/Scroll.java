package io.github.samuelfonseca.tvhome;

/** Where the home page scrolls for a focused grid tile. Plain Java, so the tests run without Android. */
final class Scroll {
    private Scroll() {}

    /**
     * From {@code current}, the least scrolling that shows {@code top..bottom} (page coordinates,
     * margins included) in a {@code viewport}-high window, then kept within {@code min..max}: min
     * puts the dock at the top of the screen, max is the end of the page.
     */
    static int toShow(int current, int top, int bottom, int viewport, int min, int max) {
        int target = Math.max(bottom - viewport, Math.min(current, top));
        return Math.min(Math.max(target, min), max);
    }
}
