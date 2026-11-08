package com.gtnhadjust.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InterfaceTerminalPreferencesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void preferencesSurviveReloadAndInvalidValuesUseFallback() {
        Path path = folder.getRoot()
            .toPath()
            .resolve("preferences.properties");
        Properties saved = new Properties();
        saved.setProperty("hideFull", "true");
        saved.setProperty("sectionOrder", "ALPHANUM");
        saved.setProperty("brokenOnly", "invalid");
        InterfaceTerminalPreferences.write(path, saved);
        Properties loaded = InterfaceTerminalPreferences.read(path);
        assertTrue(InterfaceTerminalPreferences.bool(loaded, "hideFull", false));
        assertFalse(InterfaceTerminalPreferences.bool(loaded, "brokenOnly", false));
        assertEquals("ALPHANUM", loaded.getProperty("sectionOrder"));
    }
}
