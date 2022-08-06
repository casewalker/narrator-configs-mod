/*
 * Licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 Case Walker.
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
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.casewalker.narratorconfigs.testutils.TestUtils.DummyNarrator;
import static com.casewalker.narratorconfigs.testutils.TestUtils.NarratorManagerMixinTestImpl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the functionality of {@link NarratorManagerMixinNCM2#onNarrateNCM2(String, CallbackInfo)}, {@link
 * NarratorManagerMixinNCM2#onNarrateChatMessageNCM2(Supplier, CallbackInfo)}, and {@link
 * NarratorManagerMixinNCM2#forceNarrateOnMode(Text)}.
 *
 * @author Case Walker
 */
public class NarratorManagerMixinNarrationMethodsTest {

    private static final NarratorManagerMixinTestImpl narratorManagerMixin = new NarratorManagerMixinTestImpl();
    private static ConfigHandler<NarratorConfigsModConfig> config;
    private static DummyNarrator narrator;

    @BeforeAll
    static void initializeDependencies() {
        config = new ConfigHandler<>(NarratorConfigsModConfig.class);
        config.initialize(List.of(Path.of("src", "test", "resources", "narratorconfigsmod.json")));
        Whitebox.setInternalState(narratorManagerMixin, "config", config);
        narrator = new DummyNarrator();
    }

    @BeforeEach
    void reset() {
        narrator.reset();
        narratorManagerMixin.setNarrator(narrator);
        config.get().setChatEnabled(false);
        config.get().setEnabledPrefixes(null);
        config.get().setDisabledPrefixes(null);
        config.get().setEnabledRegularExpressions(null);
    }

    @Test
    @DisplayName("Nothing is narrated by the mixin if the mode is not CUSTOM_NARRATION " +
            "(onNarrateChatMessage, onNarrate)")
    void testNarrationWrongMode() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = false;
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("text1", onNarrateCI);
        narratorManagerMixin.onNarrateChatMessageNCM2(() -> Text.of("text2"), onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations if mode is not CUSTOM_NARRATION, things said: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).toList());
        assertFalse(onNarrateCI.isCancelled(), "If the custom narration is not executed, Minecraft " +
                "'narrate' should be, and so the CallbackInfo should not be cancelled");
        assertFalse(onOnChatMessageCI.isCancelled(), "If the custom narration is not executed, Minecraft " +
                "'onChatMessage' should be, and so the CallbackInfo should not be cancelled");
    }

    @Test
    @DisplayName("Nothing is narrated if the narrator is not active (onNarrate)")
    void testNarratorInactive() {
        config.get().setChatEnabled(true);
        narrator.active = false;
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("text1", onNarrateCI);

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations when narrator is not active: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).toList());
        assertTrue(onNarrateCI.isCancelled(), "If the custom narration is executed, even with narration set " +
                "to be inactive, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Narration succeeds if the narrator is not active? Odd behavior. (onNarrateChatMessage)")
    void testNarratorInactiveChat() {
        config.get().setChatEnabled(true);
        narrator.active = false;
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateChatMessageNCM2(() -> Text.of("text2"), onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertFalse(narrator.thingsSaid.get(0).getRight(), "Interrupt on narration should be false");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even with " +
                "narration set to be inactive, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat narration succeeds (interrupt false) if the mode is right and chat is enabled " +
            "(onNarrateChatMessage)")
    void testChatSucceeds() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = true;
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateChatMessageNCM2(() -> Text.of("text2"), onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertFalse(narrator.thingsSaid.get(0).getRight(), "Interrupt on narration should be false");
        assertTrue(onOnChatMessageCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat narration does not occur if the mode is right and chat is disabled (onNarrateChatMessage)")
    void testNoNarrationIfChatIsDisabled() {
        config.get().setChatEnabled(false);
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = true;
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateChatMessageNCM2(() -> Text.of("text2"), onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "Chat is disabled, narrator should not get called");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even if chat is disabled, " +
                "the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat messages should narrate if chat is enabled regardless of what's accepted (onNarrateChatMessage)")
    void testNarrationIfChatIsEnabledEvenOnNoMatch() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateChatMessageNCM2(() -> Text.of("wrong message, not accepted"), onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "System message that matches should be narrated");
        assertTrue(onOnChatMessageCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Narration succeeds if the string matches the accepted narrations (onNarrate)")
    void testNarrationSucceedsWithRightText() {
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("testing", onNarrateCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertTrue(onNarrateCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Nothing is narrated if the string does not match an accepted narration (onNarrate)")
    void testNoNarrationIfNoMatch() {
        narrator.active = true;
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("some different string that doesn't match", onNarrateCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "The text should not be accepted, narrator should not get called");
        assertTrue(onNarrateCI.isCancelled(), "If the custom narration is executed, even if the narration was not " +
                "accepted, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("System message should not narrate if the text is accepted but the mode is wrong (forceNarrateOnMode)")
    void testNoNarrationIfSystemMessageMatchesWithBadMode() {
        narratorManagerMixin.narratorModeIsCustom = false;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));

        boolean narrated = narratorManagerMixin.forceNarrateOnMode(Text.of("testing"));

        assertTrue(narrator.thingsSaid.isEmpty(), "System message should not get narrated with wrong mode");
        assertFalse(narrated, "Since nothing was narrated, method should return false");
    }

    @Test
    @DisplayName("System message should not narrate if the text isn't accepted (forceNarrateOnMode)")
    void testNoNarrationIfSystemMessageDoesNotMatch() {
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));

        boolean narrated = narratorManagerMixin.forceNarrateOnMode(Text.of("not testing dude"));

        assertTrue(narrator.thingsSaid.isEmpty(), "System message that doesn't match should not get narrated");
        assertFalse(narrated, "Since nothing was narrated, the method should return false");
    }

    @Test
    @DisplayName("System message should narrate if the text is accepted and the mode is right (forceNarrateOnMode)")
    void testNarrationSucceedsWithRightTextAndMode() {
        narratorManagerMixin.narratorModeIsCustom = true;
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));

        boolean narrated = narratorManagerMixin.forceNarrateOnMode(Text.of("testing"));

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertTrue(narrated, "If narration succeeded, the method should return true");
    }
}
