package dev.crossroadsmc.crossroads.util;

public final class TimeFormatter {
    private TimeFormatter() {
    }

    public static String duration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "m");
        append(builder, seconds, "s");
        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, long value, String suffix) {
        if (value <= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value).append(suffix);
    }
}
