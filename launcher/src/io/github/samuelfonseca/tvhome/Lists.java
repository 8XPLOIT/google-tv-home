package io.github.samuelfonseca.tvhome;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The bundled text lists in res/raw. Plain Java, so the tests run without Android. */
final class Lists {
    private Lists() {}

    /** dock.txt: one entry per line; # starts a comment, blank lines are skipped. */
    static List<String> entries(BufferedReader r) throws IOException {
        List<String> entries = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            line = line.replaceAll("#.*", "").trim();
            if (!line.isEmpty()) entries.add(line);
        }
        return entries;
    }

    /** walls.txt: {resName, place, author, licence} per line, split on |. Other lines are skipped. */
    static List<String[]> wallpapers(BufferedReader r) throws IOException {
        List<String[]> walls = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            String[] parts = line.split("\\|");
            if (parts.length == 4) walls.add(parts);
        }
        return walls;
    }
}
