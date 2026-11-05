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
            4,
            switches.selectMixins(MODS, true)
                .size());
        assertEquals(
            3,
            switches.selectMixins(MODS, false)
                .size());
    }

    @Test
    public void disabledFeaturesDoNotSelectAnyMixin() throws Exception {
        Path file = folder.newFile()
            .toPath();
        Files.write(
            file,
            Arrays.asList("cropBreeder=false", "steamGreenhouse=false", "carpenterRecipes=false"),
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
            Arrays.asList("cropBreeder=false", "steamGreenhouse=unexpected", "carpenterRecipes=true"),
            StandardCharsets.ISO_8859_1);
        FeatureSwitches switches = FeatureSwitches.load(file);
        assertTrue(switches.carpenterRecipes);
        assertTrue(
            switches.selectMixins(MODS, true)
                .isEmpty());
        Files.write(file, Arrays.asList("cropBreeder=true", "steamGreenhouse=false"), StandardCharsets.ISO_8859_1);
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
}
