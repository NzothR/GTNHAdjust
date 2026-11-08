package com.gtnhadjust.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.gtnhadjust.GTNHAdjust;

public final class InterfaceTerminalPreferences {

    private InterfaceTerminalPreferences() {}

    public static Properties read(Path path) {
        Properties values = new Properties();
        if (!Files.exists(path)) return values;
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
        } catch (IOException | IllegalArgumentException failure) {
            GTNHAdjust.LOG.warn("Cannot read quick terminal preferences from {}", path, failure);
        }
        return values;
    }

    public static void write(Path path, Properties values) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                values.store(output, "GTNH Adjust quick terminal interface preferences");
            }
        } catch (IOException failure) {
            GTNHAdjust.LOG.warn("Cannot save quick terminal preferences to {}", path, failure);
        }
    }

    public static boolean bool(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }
}
