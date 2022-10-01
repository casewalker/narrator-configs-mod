/*
 * Licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Case Walker.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.casewalker.narratorconfigs.mixin;

import com.casewalker.modutils.config.ConfigHandler;
import com.casewalker.narratorconfigs.config.NarratorConfigsModConfig;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.util.Language;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.casewalker.narratorconfigs.testutils.TestUtils.NarratorManagerMixinTestImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests on the {@link NarratorManagerMixinNCM2} for the {@link NarratorManagerMixinNCM2#pullTranslationsFromLanguage()}
 * and {@link NarratorManagerMixinNCM2#createAcceptedNarrations(Map)} functionality.
 *
 * @author Case Walker
 */
class NarratorManagerMixinAcceptedNarrationsTest {

    private static final NarratorManagerMixinTestImpl narratorManagerMixin = new NarratorManagerMixinTestImpl();

    private static final ConfigHandler<NarratorConfigsModConfig> config =
            new ConfigHandler<>(NarratorConfigsModConfig.class);

    @BeforeAll
    static void initializeDependencies() {
        config.initialize(List.of(Path.of("src", "test", "resources", "narratorconfigsmod.json")));
        Whitebox.setInternalState(narratorManagerMixin, "config", config);
    }

    @BeforeEach
    void resetConfig() {
        config.get().setChatEnabled(false);
        config.get().setEnabledPrefixes(null);
        config.get().setDisabledPrefixes(null);
        config.get().setEnabledRegularExpressions(null);
    }

    @Test
    @DisplayName("Non TranslationStorage language returns empty map (pullTranslationsFromLanguage)")
    void testNonTranslationStorageReturnsEmptyMap() {
        Language languageMock = EasyMock.createMock(Language.class);
        EasyMock.replay(languageMock);
        Language.setInstance(languageMock);

        Map<String, String> translations = narratorManagerMixin.pullTranslationsFromLanguage();

        assertNotNull(translations, "Translations should not be null");
        assertTrue(translations.isEmpty(), "Translations should be empty from non TranslationStorage language");
    }

    @Test
    @DisplayName("TranslationStorage language will throw a class cast exception without mixin magic, but that's ok " +
            "(pullTranslationsFromLanguage)")
    void testTranslationStorageThrowsClassCastException() {
        TranslationStorage translationStorageMock = EasyMock.createMock(TranslationStorage.class);
        EasyMock.replay(translationStorageMock);
        Language.setInstance(translationStorageMock);

        assertThrows(ClassCastException.class, narratorManagerMixin::pullTranslationsFromLanguage,
                "Because of the mixin behavior, the mixin-casting is expected to throw a cast exception");
    }

    @Test
    @DisplayName("Includes specified prefixed translations (createAcceptedNarrations)")
    void testAcceptedNarrationsEnabledPrefixes() {
        config.get().setEnabledPrefixes(List.of("a"));
        Map<String, String> translations = Map.of("a", "A", "a.1", "X", "b", "B", "c", "C");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertNotNull(patterns, "Patterns should not be null");
        assertEquals(2, patterns.size(), "There should only be two returned patterns");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "A"), "The patterns should match 'A'");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "X"), "The patterns should match 'X'");
        assertFalse(narratorManagerMixin.narrationIsAccepted(patterns, "B"), "The patterns should not match 'B'");
        assertFalse(narratorManagerMixin.narrationIsAccepted(patterns, "C"), "The patterns should not match 'C'");
    }

    @Test
    @DisplayName("Excludes specified prefixed translations (createAcceptedNarrations)")
    void testAcceptedNarrationsDisabledPrefixes() {
        config.get().setEnabledPrefixes(List.of("a"));
        config.get().setDisabledPrefixes(List.of("a.2"));
        Map<String, String> translations = Map.of("a.1", "A1", "a.2", "A2", "a.3", "A3");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertNotNull(patterns, "Patterns should not be null");
        assertEquals(2, patterns.size(), "There should be two returned patterns");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "A1"), "The patterns should match 'A1'");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "A3"), "The patterns should match 'A3'");
        assertFalse(narratorManagerMixin.narrationIsAccepted(patterns, "A2"), "The patterns should not match 'A2'");
    }

    @Test
    @DisplayName("Includes custom regexes (createAcceptedNarrations)")
    void testAcceptedNarrationsCustomRegexes() {
        config.get().setEnabledRegularExpressions(List.of("^test string only$"));
        Map<String, String> translations = Map.of("a.1", "A1", "a.2", "A2", "a.3", "A3");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertNotNull(patterns, "Patterns should not be null");
        assertEquals(1, patterns.size(), "There should be one returned pattern");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "test string only"),
                "The patterns should match 'test string only'");
    }

    @Test
    @DisplayName("Escapes special characters (createAcceptedNarrations)")
    void testAcceptedNarrationsEscapesCharacters() {
        String complicatedValue = "Some ^ special $ things (on the inside) %% right? It's cool that 2 + 2 = 2 * 2. " +
                "But don't forget about pipes | and brackets like { and } [or else you may be in trouble].";
        config.get().setEnabledPrefixes(List.of("a"));
        Map<String, String> translations = Map.of("a", complicatedValue);

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertNotNull(patterns, "Patterns should not be null");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, complicatedValue),
                "The pattern should match: " + complicatedValue);
    }

    @Test
    @DisplayName("Placeholders are replaced correctly and work (createAcceptedNarrations)")
    void testAcceptedNarrationsReplacesPlaceholders() {
        config.get().setEnabledPrefixes(List.of("narrator", "death"));
        Map<String, String> translations = Map.of(
                "narrator.position.screen", "Screen element %s out of %s",
                "death.attack.inWall.player", "%1$s suffocated in a wall whilst fighting %2$s"
        );

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "Screen element 'banana' out of 25"),
                "The patterns should match a version of the screen-element translation");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "Screen element 64 out of foobar"),
                "The patterns should match another version of the screen-element translation");
        assertTrue(narratorManagerMixin.
                        narrationIsAccepted(patterns, "case_walker suffocated in a wall whilst fighting houdini"),
                "The patterns should match a version of the death translation");
        assertTrue(narratorManagerMixin.
                        narrationIsAccepted(patterns, "John_Doe suffocated in a wall whilst fighting case_walker"),
                "The patterns should match another version of the death translation");
    }

    @Test
    @DisplayName("Encloses translations correctly (createAcceptedNarrations)")
    void testAcceptedNarrationsEnclosesCorrectly() {
        config.get().setEnabledPrefixes(List.of("a"));
        Map<String, String> translations = Map.of("a", "It should match this sentence");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);

        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "It should match this sentence"),
                "Exact match should work");
        assertFalse(narratorManagerMixin.narrationIsAccepted(patterns, "abcIt should match this sentence"),
                "Match with extra characters prepended should not work");
        assertTrue(narratorManagerMixin.narrationIsAccepted(patterns, "It should match this sentence, even now"),
                "Match with extra characters post-fixed should work (after changes detected in 1.19.2)");
    }
}
