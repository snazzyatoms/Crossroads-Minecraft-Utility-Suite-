package dev.crossroadsmc.crossroads.util;

public final class DurationParser {
    private DurationParser() {
    }

    public static long parseToSeconds(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }

        String normalized = input.trim().toLowerCase();
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("forever")) {
            return Long.MAX_VALUE;
        }

        long multiplier = switch (normalized.charAt(normalized.length() - 1)) {
            case 's' -> 1L;
            case 'm' -> 60L;
            case 'h' -> 3600L;
            case 'd' -> 86400L;
            case 'w' -> 604800L;
            default -> 60L;
        };

        String numberPart = Character.isDigit(normalized.charAt(normalized.length() - 1))
            ? normalized
            : normalized.substring(0, normalized.length() - 1);

        try {
            long value = Long.parseLong(numberPart);
            return Math.max(0L, value * multiplier);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }
}
