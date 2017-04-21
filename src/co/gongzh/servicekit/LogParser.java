package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Gong Zhang
 */
public class LogParser {

    public static class Line {
        public OffsetDateTime dateTime;
        public String timestamp;
        public char level;
        public String tag;
        public String message;
    }

    @NotNull
    public static List<Line> parse(String log) {
        String[] lines = log.split("\n");
        return Arrays.stream(lines)
                .map(LogParser::parseLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    public static Line parseLine(@NotNull String line) {
        Line result = new Line();
        // "%s  %c  %s \t%s"

        // timestamp
        int i = line.indexOf("  ");
        if (i == -1) {
            return null;
        }
        try {
            result.timestamp = line.substring(0, i);
            result.dateTime = OffsetDateTime.parse(result.timestamp);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }

        // level
        line = line.substring(i + 2);
        if (line.length() < 3) {
            return null;
        }
        result.level = line.charAt(0);
        if (result.level != 'i' && result.level != 'w' && result.level != 'e') {
            return null;
        }
        if (line.charAt(1) != ' ' || line.charAt(2) != ' ') {
            return null;
        }

        // tag
        line = line.substring(3);
        i = line.indexOf(" \t");
        if (i == -1) {
            return null;
        }
        result.tag = line.substring(0, i);
        result.message = line.substring(i + 2);

        return result;
    }

}
