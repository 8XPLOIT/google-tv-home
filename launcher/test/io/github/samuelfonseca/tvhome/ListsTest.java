package io.github.samuelfonseca.tvhome;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListsTest {
    private static BufferedReader text(String s) {
        return new BufferedReader(new StringReader(s));
    }

    @Test void entriesDropCommentsBlankLinesAndSpace() throws IOException {
        List<String> got = Lists.entries(text(
                "# header\n\ncom.a.one\n  com.b.two   # trailing note\n#com.c.off\n   \n"));
        assertEquals(List.of("com.a.one", "com.b.two"), got);
    }

    @Test void entriesKeepInputIds() throws IOException {
        List<String> got = Lists.entries(text("input:com.mediatek.tvinput/.hdmi.HDMIInputService/HW5\n"));
        assertEquals(List.of("input:com.mediatek.tvinput/.hdmi.HDMIInputService/HW5"), got);
    }

    @Test void wallpapersSplitIntoFourFields() throws IOException {
        List<String[]> got = Lists.wallpapers(text("wall_03|Spullersee, Austria|Böhringer Friedrich|CC BY-SA 3.0 at\n"));
        assertEquals(1, got.size());
        assertArrayEquals(new String[] {"wall_03", "Spullersee, Austria", "Böhringer Friedrich", "CC BY-SA 3.0 at"},
                got.get(0));
    }

    @Test void wallpapersSkipMalformedLines() throws IOException {
        List<String[]> got = Lists.wallpapers(text("\nwall_01|Place|Author\nwall_02|a|b|c|d\nwall_03|a|b|c\n"));
        assertEquals(1, got.size());
        assertEquals("wall_03", got.get(0)[0]);
    }
}
