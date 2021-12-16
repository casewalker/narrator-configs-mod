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
import com.mojang.text2speech.Narrator;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.Pair;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.minecraft.network.MessageType.CHAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test functionality within the {@link NarratorManagerMixin}.
 *
 * @author Case Walker
 */
class NarratorManagerMixinTest {

    private static final NarratorManagerMixinTestImpl narratorManagerMixin = new NarratorManagerMixinTestImpl();
    private static final ConfigHandler<NarratorConfigsModConfig> config =
            new ConfigHandler<>(NarratorConfigsModConfig.class);
    private static final UUID UUID_VALUE = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @BeforeAll
    static void initializeDependencies() {
        config.initialize(List.of(Path.of("src", "test", "resources", "narratorconfigsmod.json")));
        Whitebox.setInternalState(narratorManagerMixin, "config", config);
    }

    @BeforeEach
    void resetConfig() {
        config.get().setModEnabled(false);
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

        final Map<String, String> translations = narratorManagerMixin.pullTranslationsFromLanguage();
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
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("A").matches()), "The patterns should match 'A'");
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("X").matches()), "The patterns should match 'X'");
        assertFalse(patterns.stream().anyMatch(p -> p.matcher("B").matches()), "The patterns should not match 'B'");
        assertFalse(patterns.stream().anyMatch(p -> p.matcher("C").matches()), "The patterns should not match 'C'");
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
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("A1").matches()), "The patterns should match 'A1'");
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("A3").matches()), "The patterns should match 'A3'");
        assertFalse(patterns.stream().anyMatch(p -> p.matcher("A2").matches()), "The patterns should not match 'A2'");
    }

    @Test
    @DisplayName("Includes custom regexes (createAcceptedNarrations)")
    void testAcceptedNarrationsCustomRegexes() {
        config.get().setEnabledRegularExpressions(List.of("^test string only$"));
        Map<String, String> translations = Map.of("a.1", "A1", "a.2", "A2", "a.3", "A3");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);
        assertNotNull(patterns, "Patterns should not be null");
        assertEquals(1, patterns.size(), "There should be one returned pattern");
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("test string only").matches()),
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
        assertTrue(patterns.stream().anyMatch(p -> p.matcher(complicatedValue).matches()),
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
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("Screen element 'banana' out of 25").matches()),
                "The patterns should match a version of the screen-element translation");
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("Screen element 64 out of foobar").matches()),
                "The patterns should match another version of the screen-element translation");
        assertTrue(patterns.stream().anyMatch(p ->
                        p.matcher("case_walker suffocated in a wall whilst fighting houdini").matches()),
                "The patterns should match a version of the death translation");
        assertTrue(patterns.stream().anyMatch(p ->
                        p.matcher("John_Doe suffocated in a wall whilst fighting case_walker").matches()),
                "The patterns should match another version of the death translation");
    }

    @Test
    @DisplayName("Encloses translations correctly (createAcceptedNarrations)")
    void testAcceptedNarrationsEnclosesCorrectly() {
        config.get().setEnabledPrefixes(List.of("a"));
        Map<String, String> translations = Map.of("a", "It should match this sentence");

        Set<Pattern> patterns = narratorManagerMixin.createAcceptedNarrations(translations);
        assertTrue(patterns.stream().anyMatch(p -> p.matcher("It should match this sentence").matches()),
                "Exact match should work");
        assertFalse(patterns.stream().anyMatch(p -> p.matcher("abcIt should match this sentence").matches()),
                "Match with extra characters prepended should not work");
        assertFalse(patterns.stream().anyMatch(p -> p.matcher("It should match this sentence, well not now").matches()),
                "Match with extra characters postfixed should not work");
    }

    @Test
    @DisplayName("Nothing is narrated if the mod is disabled (onOnChatMessage, onNarrate)")
    public void testModDisabled() {
        config.get().setChatEnabled(true);
        DummyNarrator narrator = new DummyNarrator();
        narrator.active = true;
        narratorManagerMixin.setNarrator(narrator);

        narratorManagerMixin.onNarrate("text1", new CallbackInfo("test", true));
        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, new CallbackInfo("test", true));

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations if mod is disabled: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("Nothing is narrated if the narrator is not active (onOnChatMessage, onNarrate)")
    public void testNarratorInactive() {
        config.get().setModEnabled(true);
        config.get().setChatEnabled(true);
        DummyNarrator narrator = new DummyNarrator();
        narrator.active = false;
        narratorManagerMixin.setNarrator(narrator);

        narratorManagerMixin.onNarrate("text1", new CallbackInfo("test", true));
        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, new CallbackInfo("test", true));

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations when narrator is not active: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("Chat narration succeeds (interrupt false) if the mod and chat are enabled (onOnChatMessage)")
    public void testChatSucceeds() {
        config.get().setModEnabled(true);
        config.get().setChatEnabled(true);
        DummyNarrator narrator = new DummyNarrator();
        narrator.active = true;
        narratorManagerMixin.setNarrator(narrator);
        CallbackInfo ci = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text"), UUID_VALUE, ci);

        assertEquals(1, narrator.thingsSaid.size(), "Narrator should have received 1 narration");
        assertFalse(narrator.thingsSaid.get(0).getRight(), "Interrupt should be false for chat message");
        assertTrue(ci.isCancelled(), "CallbackInfo should become canceled if chat is narrated");
    }

    /**
     * Concrete implementation for the {@link NarratorManagerMixin} abstract class.
     */
    private static class NarratorManagerMixinTestImpl extends NarratorManagerMixin {
        void debugPrintMessage(String var1) {}
        void setNarrator(Narrator narrator) { Whitebox.setInternalState(this, "narrator", narrator); }
    }

    /**
     * Class to mock the narrator.
     */
    private static class DummyNarrator implements Narrator {
        public boolean active;
        public List<Pair<String, Boolean>> thingsSaid = new ArrayList<>();

        public void say(String msg, boolean interrupt) { thingsSaid.add(new Pair<>(msg, interrupt)); }
        public void clear() {}
        public boolean active() { return active; }
        public void destroy() {}
    }
}
