package org.GlassBishop.engine;

import java.util.Locale;

public enum Difficulty {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5);

    private final int searchDepth;

    Difficulty(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    public int searchDepth() {
        return searchDepth;
    }

    public static Difficulty parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("[1-5]")) {
            return values()[Integer.parseInt(normalized) - 1];
        }
        try {
            return switch (normalized) {
                case "EASY" -> LEVEL_1;
                case "MEDIUM" -> LEVEL_2;
                case "HARD" -> LEVEL_4;
                default -> valueOf(normalized);
            };
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown search depth '" + value + "'. Choose a value from 1 to 5.", exception);
        }
    }
}
