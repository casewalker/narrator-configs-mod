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
import net.minecraft.network.message.MessageSender;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.RegistryKey;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.casewalker.narratorconfigs.testutils.TestUtils.DummyNarrator;
import static com.casewalker.narratorconfigs.testutils.TestUtils.NarratorManagerMixinTestImpl;
import static com.casewalker.narratorconfigs.testutils.TestUtils.assertFalse;
import static com.casewalker.narratorconfigs.testutils.TestUtils.assertTrue;
import static net.minecraft.network.message.MessageType.NarrationRule.Kind.CHAT;
import static net.minecraft.network.message.MessageType.NarrationRule.Kind.SYSTEM;

/**
 * Tests on the {@link NarratorManagerMixinNCM2} for the {@link
 * NarratorManagerMixinNCM2#onNarrateNCM2(String, CallbackInfo)} and {@link
 * NarratorManagerMixinNCM2#onOnChatMessageNCM2(MessageType, Text, MessageSender, CallbackInfo)} functionality.
 *
 * This class is using JUnit4 and the {@link PowerMockRunner} in order to mock {@link MessageType}. I could not find a
 * way around this using JUnit5. Thus the usage of {@link DisplayName} is defunct, but a boy can dream.
 *
 * @author Case Walker
 */
@PrepareForTest({
        MessageType.class,
        MessageType.NarrationRule.class,
        RegistryKey.class
})
@SuppressStaticInitializationFor("net.minecraft.util.registry.Registry")
@RunWith(PowerMockRunner.class)
public class NarratorManagerMixinNarrationMethodsTest {

    private static NarratorManagerMixinTestImpl narratorManagerMixin;
    private static ConfigHandler<NarratorConfigsModConfig> config;
    private static MessageType chatType;
    private static MessageType systemType;
    private static DummyNarrator narrator;

    @BeforeClass
    public static void initializeDependencies() {
        narratorManagerMixin = PowerMock.createPartialMock(NarratorManagerMixinTestImpl.class, "narratorModeIsCustomNarration");
        config = new ConfigHandler<>(NarratorConfigsModConfig.class);
        config.initialize(List.of(Path.of("src", "test", "resources", "narratorconfigsmod.json")));
        Whitebox.setInternalState(narratorManagerMixin, "config", config);
        narrator = new DummyNarrator();

        // This remaining nonsense is because Mojang introduced "MessageType" as a Record (I believe serialized) for
        // 1.19, and it proved to be very difficult to mock. Mockito complained, PowerMockRunner doesn't exist for
        // JUnit5, so I had to downgrade to JUnit4, put the "add-exports" and "add-opens" in gradle, etc.
        // It's giving depression. But now it should work.
        PowerMock.mockStatic(RegistryKey.class);
        EasyMock.expect(RegistryKey.of(EasyMock.anyObject(), EasyMock.anyObject())).andStubReturn(null);
        PowerMock.replay(RegistryKey.class);

        final Optional<MessageType.NarrationRule> chatRule = Optional.of(MessageType.NarrationRule.of(CHAT));
        final Optional<MessageType.NarrationRule> systemRule = Optional.of(MessageType.NarrationRule.of(SYSTEM));
        chatType = PowerMock.createMock(MessageType.class, Optional.empty(), Optional.empty(), chatRule);
        systemType = PowerMock.createMock(MessageType.class, Optional.empty(), Optional.empty(), systemRule);

        EasyMock.expect(chatType.narration()).andStubReturn(Optional.of(MessageType.NarrationRule.of(CHAT)));
        EasyMock.expect(systemType.narration()).andStubReturn(Optional.of(MessageType.NarrationRule.of(SYSTEM)));
        PowerMock.replayAll(chatType, systemType);
    }

    @Before
    public void reset() {
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
    public void testNarrationWrongMode() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(false);
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("text1", onNarrateCI);
        narratorManagerMixin.onOnChatMessageNCM2(chatType, Text.of("text2"), null, onOnChatMessageCI);

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
    public void testNarratorInactive() {
        config.get().setChatEnabled(true);
        narrator.active = false;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("text1", onNarrateCI);
        narratorManagerMixin.onOnChatMessageNCM2(chatType, Text.of("text2"), null, onOnChatMessageCI);

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
    public void testChatSucceeds() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessageNCM2(chatType, Text.of("text2"), null, onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertFalse(narrator.thingsSaid.get(0).getRight(), "Interrupt on narration should be false");
        assertTrue(onOnChatMessageCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Chat narration does not occur if the mode is right and chat is disabled (onOnChatMessage)")
    public void testNoNarrationIfChatIsDisabled() {
        config.get().setChatEnabled(false);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessageNCM2(chatType, Text.of("text2"), null, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "Chat is disabled, narrator should not get called");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even if chat is disabled, " +
                "the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("System messages should narrate if chat is disabled and the text is accepted (onOnChatMessage)")
    public void testNarrationIfChatIsDisabledButSystemMessageMatches() {
        config.get().setChatEnabled(false);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessageNCM2(systemType, Text.of("testing"), null, onOnChatMessageCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "System message that matches should be narrated");
        assertTrue(onOnChatMessageCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("System messages should not narrate if chat is disabled but the text isn't accepted (onOnChatMessage)")
    public void testNoNarrationIfChatIsDisabledAndSystemMessageDoesNotMatch() {
        config.get().setChatEnabled(false);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessageNCM2(systemType, Text.of("not testing dude"), null, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "System message that doesn't match should not get narrated");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even if nothing is " +
                "narrated, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("System messages should not narrate even if chat is enabled but the text isn't accepted " +
            "(onOnChatMessage)")
    public void testNoNarrationIfChatIsEnabledAndSystemMessageDoesNotMatch() {
        config.get().setChatEnabled(true);
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onOnChatMessageCI = new CallbackInfo("test", true);

        narratorManagerMixin.onOnChatMessageNCM2(systemType, Text.of("not testing either"), null, onOnChatMessageCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "System message that doesn't match should not get narrated");
        assertTrue(onOnChatMessageCI.isCancelled(), "If the custom narration is executed, even if nothing is " +
                "narrated, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Narration succeeds if the string matches the accepted narrations (onNarrate)")
    public void testNarrationSucceedsWithRightText() {
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("testing", onNarrateCI);

        assertFalse(narrator.thingsSaid.isEmpty(), "Narrator should get called with things to say");
        assertTrue(onNarrateCI.isCancelled(),
                "If the custom narration is executed, the CallbackInfo should be cancelled");
    }

    @Test
    @DisplayName("Nothing is narrated if the string does not match an accepted narration (onNarrate)")
    public void testNoNarrationIfNoMatch() {
        narrator.active = true;
        setupIsCustomNarrationModeExpectation(true);
        Whitebox.setInternalState(narratorManagerMixin, "acceptedNarrations", Set.of(Pattern.compile("^testing$")));
        CallbackInfo onNarrateCI = new CallbackInfo("test", true);

        narratorManagerMixin.onNarrateNCM2("some different string that doesn't match", onNarrateCI);

        assertTrue(narrator.thingsSaid.isEmpty(), "The text should not be accepted, narrator should not get called");
        assertTrue(onNarrateCI.isCancelled(), "If the custom narration is executed, even if the narration was not " +
                "accepted, the CallbackInfo should be cancelled");
    }

    /**
     * Helper method to set up the partial {@link NarratorManagerMixinNCM2} mock.
     *
     * @param result The desired result for the {@link NarratorManagerMixinNCM2#narratorModeIsCustomNarration()} method
     */
    private void setupIsCustomNarrationModeExpectation(final boolean result) {
        EasyMock.expect(narratorManagerMixin.narratorModeIsCustomNarration()).andStubReturn(result);
        EasyMock.replay(narratorManagerMixin);
    }
}
