package com.gtnhadjust;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FeatureSwitchesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private static final Set<String> MODS = new HashSet<>(
        Arrays.asList("gtneverreset", "sciencenotleisure", "cropsnh"));

    @Test
    public void defaultsCreateFileAndSelectAll() throws Exception {
        Path file = folder.getRoot()
            .toPath()
            .resolve("config/features.properties");
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertTrue(Files.isRegularFile(file));
        assertTrue(switches.carpenterRecipes);
        assertEquals(
            9,
            switches.selectMixins(MODS, true)
                .size());
        assertEquals(
            5,
            switches.selectMixins(MODS, false)
                .size());
    }

    @Test
    public void disabledFeaturesDoNotSelectAnyMixin() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays
                .asList("cropBreeder=false", "steamGreenhouse=false", "carpenterRecipes=false", "quickTerminal=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertFalse(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
        assertTrue(
            switches.selectMixins(MODS, false)
                .isEmpty());
    }

    @Test
    public void featuresCanBeDisabledIndependentlyAndInvalidValuesFailClosed() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList(
                "cropBreeder=false",
                "steamGreenhouse=unexpected",
                "carpenterRecipes=true",
                "quickTerminal=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertTrue(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
        Files.write(
            file,
            Arrays.asList("cropBreeder=true", "steamGreenhouse=false", "quickTerminal=false"),
            StandardCharsets.ISO_8859_1);
        List<String> mixins = FeatureSwitches.load(file)
            .selectMixins(MODS, true);
        assertEquals(Arrays.asList("CropBreedingRulesMixin", "LargeCropBreederMixin"), mixins);
    }

    @Test
    public void unreadableConfigurationFailsClosed() throws Exception {
        Path directory = folder.newFolder()
            .toPath();
        FeatureSwitches switches = FeatureSwitches.load(directory);
        assertFalse(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
    }

    @Test
    public void terminalMixinsAreOneSwitchAcrossBothSides() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList("cropBreeder=false", "steamGreenhouse=false", "quickTerminal=true"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches enabled = FeatureSwitches.load(file);
        assertEquals(
            5,
            enabled.selectMixins(MODS, true)
                .size());
        assertEquals(
            2,
            enabled.selectMixins(MODS, false)
                .size());
        assertTrue(
            enabled.selectMixins(new HashSet<>(), true)
                .isEmpty());
        Files.write(
            file,
            Arrays.asList("cropBreeder=false", "steamGreenhouse=false", "quickTerminal=false"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches disabled = FeatureSwitches.load(file);
        assertTrue(
            disabled.selectMixins(MODS, true)
                .isEmpty());
        assertTrue(
            disabled.selectMixins(MODS, false)
                .isEmpty());
    }

    @Test
    public void gtsrDiagnosticsCanBeDisabledWithoutDisablingOtherFeatures() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Set<String> mods = new HashSet<>(Arrays.asList("gtsr"));
        assertEquals(
            Arrays.asList("diagnostic.GtsrRecipeAuditMixin"),
            FeatureSwitches.load(file)
                .selectMixins(mods, false));
        Files.write(file, Arrays.asList("gtsrDiagnostics=false"), StandardCharsets.ISO_8859_1);
        assertTrue(
            FeatureSwitches.load(file)
                .selectMixins(mods, false)
                .isEmpty());
    }
}
