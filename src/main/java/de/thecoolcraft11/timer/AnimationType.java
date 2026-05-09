package de.thecoolcraft11.timer;

public enum AnimationType {
    GRADIENT,
    WAVE,
    PULSE,
    RAINBOW,
    STILL;

    public static AnimationType fromString(String value) {
        return switch (value.toLowerCase()) {
            case "wave" -> WAVE;
            case "pulse" -> PULSE;
            case "rainbow" -> RAINBOW;
            case "still" -> STILL;
            default -> GRADIENT;
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
