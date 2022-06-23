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
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.powermock.api.easymock.PowerMock;
import org.powermock.reflect.Whitebox;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.casewalker.narratorconfigs.testutils.TestUtils.DummyNarrator;
import static com.casewalker.narratorconfigs.testutils.TestUtils.NarratorManagerMixinTestImpl;
import static net.minecraft.network.MessageType.CHAT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests on the {@link NarratorManagerMixin} for the {@link NarratorManagerMixin#onNarrate(String, CallbackInfo)} and
 * {@link NarratorManagerMixin#onOnChatMessage(MessageType, Text, UUID, CallbackInfo)} functionality.
 *
 * @author Case Walker
 */
class NarratorManagerMixinNarrationsTest {

    private static final NarratorManagerMixinTestImpl narratorManagerMixin =
            PowerMock.createPartialMock(NarratorManagerMixinTestImpl.class, "narratorModeIsCustomNarration");

    private static final ConfigHandler<NarratorConfigsModConfig> config =
            new ConfigHandler<>(NarratorConfigsModConfig.class);

    private static final UUID UUID_VALUE = UUID.fromString("12345678-1234-1234-1234-123456789012");

    private static final DummyNarrator narrator = new DummyNarrator();

    @BeforeAll
    static void initializeDependencies() {
        config.initialize(List.of(Path.of("src", "test", "resources", "narratorconfigsmod.json")));
        Whitebox.setInternalState(narratorManagerMixin, "config", config);
    }

    @BeforeEach
    void reset() {
        EasyMock.reset(narratorManagerMixin);
        narrator.reset();
        narratorManagerMixin.setNarrator(narrator);
        config.get().setChatEnabled(false);
        config.get().setEnabledPrefixes(null);
        config.get().setDisabledPrefixes(null);
        config.get().setEnabledRegularExpressions(null);
    }

    @Test
    @DisplayName("Nothing is narrated by the mixin if the mode is not CUSTOM_NARRATION (onOnChatMessage, onNarrate)")
    void testNarrationWrongMode() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(false);
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrate("text1", onNarrateCI);
        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations if mode is not CUSTOM_NARRATION, things said: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).collect(Collectors.toList()));
        assertFalse(onNarrateCI.isCancelled(), "If the custom narration is not executed, Minecraft " +
                "'narrate' should be, and so the CallbackInfo should not be cancelled");
        assertFalse(onOnChatMessageCI.isCancelled(), "If the custom narration is not executed, Minecraft " +
                "'onChatMessage' should be, and so the CallbackInfo should not be cancelled");
    }

    @Test
    @DisplayName("Nothing is narrated if the narrator is not active (onOnChatMessage, onNarrate)")
    void testNarratorInactive() {
        config.get().setChatEnabled(true);
        narrator.active = false;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrate("text1", onNarrateCI);
        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(),
                "Narrator should not get any narrations when narrator is not active: " +
                        narrator.thingsSaid.stream().map(Pair::getLeft).collect(Collectors.toList()));
        assertTrue(onNarrateCI.isCancelled(), "If the custom narration is executed, even with narration set " +
                "to be inactive, the CallbackInfo should be cancelled");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even with " +
                "narration set to be inactive, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat narration succeeds (interrupt false) if the mode is right and chat is enabled (onOnChatMessage)")
    void testChatSucceeds() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertFalse(narrator.thingsSaid.get(0).getRight(), "Interrupt on narration should be false");
        assertTrue(onOnChatMessageCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat narration does not occur if the mode is right and chat is disabled (onOnChatMessage)")
    void testNoNarrationIfChatIsDisabled() {
        config.get().setChatEnabled(false);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessage(CHAT, Text.of("text2"), UUID_VALUE, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "Chat is disabled, narrator should not get called");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even if chat is disabled, " +
                "the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Narration succeeds if the string matches the accepted narrations (onNarrate)")
    void testNarrationSucceedsWithRightText() {
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrate("testing", onNarrateCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertTrue(onNarrateCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Nothing is narrated if the string does not match an accepted narration (onNarrate)")
    void testNoNarrationIfNoMatch() {
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrate("some different string that doesn't match", onNarrateCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "The text should not be accepted, narrator should not get called");
        assertTrue(onNarrateCI.isCancelled(), "If the custom narration is executed, even if the narration was not " +
                "accepted, the CallbackInfo should be cancelled");
    }

    /**
     * Helper method to set up the partial {@link NarratorManagerMixin} mock.
     *
     * @param result The desired result for the {@link NarratorManagerMixin#narratorModeIsCustomNarration()} method
     */
    private void setupIsCustomNarrationModeExpectation(final boolean result) {
        EasyMock.expect(narratorManagerMixin.narratorModeIsCustomNarration()).andStubReturn(result);
        EasyMock.replay(narratorManagerMixin);
    }
}
