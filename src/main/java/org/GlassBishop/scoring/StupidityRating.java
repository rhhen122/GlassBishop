package org.GlassBishop.scoring;

public enum StupidityRating {
    RS(0, 10),
    SR(11, 19),
    SS(20, 29),
    MS(30, 39),
    PB(40, 49),
    BS(50, 59),
    LaB(60, 69),
    LI(70, 79),
    WB(80, 85),
    BFC(86, 95),
    ZE(96, 100);

    private final int minimum;
    private final int maximum;

    StupidityRating(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static StupidityRating forScore(int score) {
        for (StupidityRating rating : values()) {
            if (score >= rating.minimum && score <= rating.maximum) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Score must be between 0 and 100: " + score);
    }
}
