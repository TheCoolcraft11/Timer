package de.thecoolcraft11.timer;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TimerUtil {

    private static final List<String> WORLD_SUBFOLDERS = List.of(
            "data",
            "data/minecraft",
            "datapacks",
            "playerdata",
            "stats",
            "advancements"
    );

    public static String interpolateColor(String color1, String color2, float ratio) {
        int r1 = Integer.parseInt(color1.substring(1, 3), 16);
        int g1 = Integer.parseInt(color1.substring(3, 5), 16);
        int b1 = Integer.parseInt(color1.substring(5, 7), 16);

        int r2 = Integer.parseInt(color2.substring(1, 3), 16);
        int g2 = Integer.parseInt(color2.substring(3, 5), 16);
        int b2 = Integer.parseInt(color2.substring(5, 7), 16);

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return String.format("#%02X%02X%02X", r, g, b);
    }

    @NonNull
    public static String formatTime(long currentTime) {
        long hours = currentTime / 3600;
        long minutes = (currentTime % 3600) / 60;
        long seconds = currentTime % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static String hslToHex(int hue) {
        float h = hue / 360.0f;
        float s = 100 / 100.0f;
        float l = 50 / 100.0f;

        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;

        if (h < 1.0f / 6) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 2.0f / 6) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 3.0f / 6) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 4.0f / 6) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 5.0f / 6) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int red = (int) ((r + m) * 255);
        int green = (int) ((g + m) * 255);
        int blue = (int) ((b + m) * 255);

        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public static boolean isWorldFolder(Path folder) {
        return Files.isDirectory(folder) && Files.isRegularFile(folder.resolve("level.dat"));
    }

    public static Set<String> resolveWorldFolders(Path worldContainer, List<String> patterns) {
        List<String> existingWorlds = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldContainer)) {
            for (Path path : stream) {
                if (isWorldFolder(path)) {
                    existingWorlds.add(path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            return new LinkedHashSet<>();
        }

        Set<String> resolved = new LinkedHashSet<>();
        for (String pattern : patterns) {
            if (pattern.contains("*")) {
                for (String worldName : existingWorlds) {
                    if (matchesPattern(worldName, pattern)) {
                        resolved.add(worldName);
                    }
                }
            } else {
                resolved.add(pattern);
            }
        }
        return resolved;
    }

    public static boolean matchesPattern(String worldName, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }

        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");

        return worldName.matches(regex);
    }

    public static boolean deleteWorldFolder(Path worldContainer, String worldName) {
        Path folder = worldContainer.resolve(worldName);
        if (Files.exists(folder) && !deleteDirectory(folder)) {
            return false;
        }

        
        
        
        
        try {
            Files.createDirectories(folder);
            for (String subfolder : WORLD_SUBFOLDERS) {
                Files.createDirectories(folder.resolve(subfolder));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }

                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
