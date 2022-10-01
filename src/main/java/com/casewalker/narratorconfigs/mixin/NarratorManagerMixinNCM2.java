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
import com.casewalker.narratorconfigs.interfaces.AccessibleTranslationStorage;
import com.casewalker.modutils.interfaces.Reloadable;
import com.casewalker.narratorconfigs.interfaces.ForcedNarratorManagerNCM2;
import com.casewalker.narratorconfigs.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.mojang.text2speech.Narrator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.casewalker.narratorconfigs.NarratorConfigsMod.LOGGER;
import static com.casewalker.narratorconfigs.NarratorConfigsMod.MOD_NAME;

/**
 * Mixin targeting the {@link NarratorManager} class to allow users to have more fine-grained control over what text is
 * narrated and what text is not.
 * <p>
 * The existing narrator in Minecraft allows for three narration modes:
 * <ul>
 *     <li>Narrates All</li>
 *     <li>Narrates Chat</li>
 *     <li>Narrates System</li>
 * </ul>
 * Some users find these choices to be limited; "Narrates All" can be very noisy (along with "Narrates System"), while
 * "Narrates Chat" misses certain system messages that appear alongside user chats. The purpose of this mixin and mod is
 * to allow users to specify exactly which text they would like to have narrated, given the choice to enable and disable
 * chat separately from enabling and disabling specific texts defined from a Minecraft language JSON file. As well, if
 * users wish to give specific regular expressions to test against potential narrations, that is also accepted.
 *
 * @author Case Walker
 */
@Mixin(NarratorManager.class)
public abstract class NarratorManagerMixinNCM2 implements ForcedNarratorManagerNCM2, Reloadable {

    @Shadow
    @Final
    private Narrator narrator;

    @Shadow
    protected abstract void debugPrintMessage(String var1);

    @Shadow
    private NarratorMode getNarratorOption() {
        throw new AssertionError("Shadowed method wrapper 'getNarratorOption' should not run");
    }

    /**
     * Wrapper method to make testing easy and provide the answer this mod cares about, whether the current narrator
     * option is equal to the CUSTOM_NARRATION mode.
     *
     * @return The result of {@link #getNarratorOption()} == CUSTOM_NARRATION
     */
    @VisibleForTesting
    protected boolean narratorModeIsCustomNarration() {
        return getNarratorOption().equals(Util.customNarration());
    }

    /**
     * Set of acceptable narrations based on the configured enabled prefixes.
     */
    private Set<Pattern> acceptedNarrations = Collections.emptySet();

    /**
     * Configuration.
     */
    private ConfigHandler<NarratorConfigsModConfig> config;

    /**
     * Inject custom logic at the end of {@link NarratorManager#NarratorManager(MinecraftClient)}. This logic will try
     * to construct a set of acceptable narrations by:
     * <ul>
     *     <li>Filtering all translations by their keys to see if the key matches one of the configured enabled
     *     prefixes</li>
     *     <li>Filtering all translations by their keys to see that the key does not match one of the configured
     *     disabled prefixes</li>
     *     <li>Tweaking the narrations (translations' values) to replace all placeholders ("%s", "%d", "%1$s", etc.)
     *     with ".*"</li>
     *     <li>Adding "^" and "$" characters to the start and end to bound the translations when they become
     *     patterns</li>
     *     <li>Converting the altered narration strings into {@link Pattern}s</li>
     *     <li>Also adding any exactly specified regexes to the narration-testing patterns</li>
     * </ul>
     *
     * @param ci {@link CallbackInfo} used by SpongePowered
     */
    @SuppressWarnings("BusyWait")
    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInitNCM2(final CallbackInfo ci) {
        LOGGER.info("This line is printed by the Narrator Configs Mod mixin!");

        config = new ConfigHandler<>(NarratorConfigsModConfig.class);
        config.initialize();
        config.registerSubscriber(this);

        // Wait in a separate thread for the TranslationStorage to be loaded
        new Thread(() -> {
            Map<String, String> translations = Collections.emptyMap();
            while (translations.isEmpty()) {
                try {
                    LOGGER.info("Sleep before pulling translations...");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupt while waiting to load translations", e);
                }
                LOGGER.info("Trying to pull translations...");
                translations = pullTranslationsFromLanguage();
            }
            reload();
        }).start();
    }

    /**
     * Inject a narration override at the head of {@link NarratorManager#narrateChatMessage(Supplier)}.
     *
     * @param messageSupplier Text (supplied) to optionally narrate from the Minecraft chat
     * @param ci {@link CallbackInfo} used by SpongePowered
     */
    @Inject(method = "narrateChatMessage", at = @At("HEAD"), cancellable = true)
    public void onNarrateChatMessageNCM2(final Supplier<Text> messageSupplier, final CallbackInfo ci) {

        // If the narrator mode is not CUSTOM_NARRATION, exit and run the underlying Minecraft method
        if (!narratorModeIsCustomNarration()) {
            return;
        }

        // Copied mostly from NarratorManager#narrateChatMessage. TODO Why is there no 'this.narrator.active()' check?

        if (config.get().isChatEnabled()) {
            final String string = messageSupplier.get().getString();
            this.debugPrintMessage(string);
            this.narrator.say(string, false);
        }

        // If the mixin was called with the right NarratorMode, then cancel the call to narrateChatMessage
        ci.cancel();
    }

    /**
     * Inject a narration override at the head of {@link NarratorManager#narrate(String)}.
     *
     * @param text Text to optionally narrate
     * @param ci   {@link CallbackInfo} used by SpongePowered
     */
    @Inject(method = "narrate(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    public void onNarrateNCM2(final String text, final CallbackInfo ci) {

        // If the narrator mode is not CUSTOM_NARRATION, exit and run the underlying Minecraft method
        if (!narratorModeIsCustomNarration()) {
            return;
        }

        // Check that the text to narrate is not empty and matches one of the narrations extrapolated from the config
        if (!text.isEmpty() && narrationIsAccepted(text)) {
            debugPrintMessage(text);
            if (narrator.active()) {
                narrator.clear();
                narrator.say(text, true);
            }
        }
        // If the mixin was called with the right NarratorMode, then cancel the call to NarratorManager#narrate
        ci.cancel();
    }

    @Override
    public boolean forceNarrateOnMode(final Text text) {
        final String string = text.getString();

        if (narratorModeIsCustomNarration() && narrationIsAccepted(string)) {
            this.narrator.say(text.getString(), false);
            return true;
        }
        return false;
    }


    /**
     * Reload accepted narrations in the event that when the class was instantiated, the {@link Language} was not
     * properly set, or the mod configuration file was updated and this class is being reloaded as its subscriber.
     */
    @Override
    public void reload() {
        final Map<String, String> translations = pullTranslationsFromLanguage();
        acceptedNarrations = createAcceptedNarrations(translations);
        if (!narrator.active()) {
            debugPrintMessage("Updated configuration: " + config.get());
        } else {
            narrator.say("Narrator configuration has updated from the config file", false);
        }
    }

    /**
     * Attempt to get the translations stored in {@link TranslationStorage} out of the {@link Language} base class.
     *
     * @return The translations as stored in a TranslationStorage, which should be generated from a language file such
     * as en_us.json
     */
    @VisibleForTesting
    protected Map<String, String> pullTranslationsFromLanguage() {
        final Language language = Language.getInstance();
        if (!(language instanceof TranslationStorage)) {
            LOGGER.error("Language is not an instance of {}, {} will not work correctly when using prefixes",
                    TranslationStorage.class.getSimpleName(),
                    MOD_NAME);
            return Collections.emptyMap();
        }
        LOGGER.info("{} successfully pulled translations", MOD_NAME);
        return ((AccessibleTranslationStorage) language).getTranslations();
    }

    /**
     * Create the accepted narrations by combining the provided translations map with the configurations. See
     * {@link #onInitNCM2(CallbackInfo)} for more details.
     *
     * @param translations Map of keys and values such as in en_us.json
     * @return Translations combined and manipulated based on configurations
     */
    @VisibleForTesting
    protected Set<Pattern> createAcceptedNarrations(final Map<String, String> translations) {

        final Set<Pattern> output = translations.entrySet().stream()
                // filter in all enabled prefixes
                .filter(entry -> config.get().getEnabledPrefixes().stream()
                                .anyMatch(enabledPrefix -> entry.getKey().startsWith(enabledPrefix)))
                // filter out any disabled prefixes
                .filter(entry -> config.get().getDisabledPrefixes().stream()
                                .noneMatch(disabledPrefix -> entry.getKey().startsWith(disabledPrefix)))
                .map(Map.Entry::getValue)
                // escape all special characters in the language translations
                .map(translation -> translation.replaceAll("([]\\[.()^$*+?{}|])", "\\\\$1"))
                // replace all placeholders (accounting for escapes added above) like '%s' with the regex '.*'
                .map(translation -> translation.replaceAll("%(\\d+\\\\[$])?[sd]", ".*"))
                // encase the string as a pattern with a beginning and a wildcard end
                .map(translationPattern -> "^" + translationPattern + ".*")
                // finally Pattern.compile() and collect
                .map(Pattern::compile)
                .collect(Collectors.toSet());

        // add all configured Enabled Regular Expressions to the accepted narration matchers
        output.addAll(
                config.get().getEnabledRegularExpressions().stream().map(Pattern::compile).collect(Collectors.toList())
        );

        LOGGER.info("accepted narrations: {}", output);
        return output;
    }

    /**
     * Check whether a narration is accepted given the consumed translations and mod configurations.
     *
     * @param string Message to possibly be narrated
     * @return Whether the narration is accepted
     */
    private boolean narrationIsAccepted(final String string) {
        return narrationIsAccepted(acceptedNarrations, string);
    }

    /**
     * See {@link #narrationIsAccepted(String)}.
     *
     * @param acceptedNarrations Patterns to check the message against
     * @param string Message to possibly be narrated
     * @return Whether the narration is accepted
     */
    @VisibleForTesting
    protected boolean narrationIsAccepted(final Set<Pattern> acceptedNarrations, final String string) {
        return acceptedNarrations.stream().anyMatch(pattern -> pattern.matcher(string).matches());
    }
}
