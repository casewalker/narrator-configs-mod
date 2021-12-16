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
package com.casewalker.narratorconfigs.config;

import com.casewalker.modutils.config.AbstractConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configurations for the mod. Extends {@link AbstractConfig} and is intended to be used with
 * {@link com.casewalker.modutils.config.ConfigHandler}.
 *
 * @author Case Walker
 */
public class NarratorConfigsModConfig extends AbstractConfig {

    private static final String BASE_FILENAME = "narratorconfigsmod";
    private static final Path DEFAULT_JSON_CONFIG = Path.of("config", BASE_FILENAME + ".json");
    private static final Path DEFAULT_YAML_CONFIG = Path.of("config", BASE_FILENAME + ".yml");
    private static final Path OTHER_DEFAULT_YAML_CONFIG = Path.of("config", BASE_FILENAME + ".yaml");

    private boolean modEnabled;
    private boolean chatEnabled;
    private List<String> enabledPrefixes;
    private List<String> disabledPrefixes;
    private List<String> enabledRegularExpressions;

    @Override
    public List<Path> getDefaultConfigPaths() {
        return List.of(DEFAULT_JSON_CONFIG, DEFAULT_YAML_CONFIG, OTHER_DEFAULT_YAML_CONFIG);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NarratorConfigsModConfig that = (NarratorConfigsModConfig) o;
        // Determine equality based on just the config properties
        return modEnabled == that.modEnabled && chatEnabled == that.chatEnabled &&
                Objects.equals(enabledPrefixes, that.enabledPrefixes) &&
                Objects.equals(disabledPrefixes, that.disabledPrefixes) &&
                Objects.equals(enabledRegularExpressions, that.enabledRegularExpressions);
    }

    /**
     * Provide a string representation of the configuration which can be narrated to the player.
     *
     * @return The string representation of the config
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Narrator Configs Mod Configuration: The mod is ");
        sb.append(modEnabled ? "enabled" : "disabled");
        sb.append(", Chat is ");
        sb.append(chatEnabled ? "enabled" : "disabled");
        sb.append(", ");
        if (enabledPrefixes == null || enabledPrefixes.isEmpty()) {
            sb.append("No prefixes are enabled");
        } else {
            sb.append("Enabled Prefixes include ");
            sb.append(enabledPrefixes);
        }
        sb.append(", ");
        if (disabledPrefixes == null || disabledPrefixes.isEmpty()) {
            sb.append("No prefixes are disabled");
        } else {
            sb.append("Disabled Prefixes include ");
            sb.append(disabledPrefixes);
        }
        sb.append( ", and ");
        if (enabledRegularExpressions == null || enabledRegularExpressions.isEmpty()) {
            sb.append("No regular expressions are enabled");
        } else {
            sb.append("Custom Regular Expressions include ");
            sb.append(enabledRegularExpressions);
        }
        sb.append(".");
        return sb.toString();
    }

    public boolean isModEnabled() {
        return modEnabled;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public List<String> getEnabledPrefixes() {
        return enabledPrefixes == null ? Collections.emptyList() : Collections.unmodifiableList(enabledPrefixes);
    }

    public List<String> getDisabledPrefixes() {
        return disabledPrefixes == null ? Collections.emptyList() : Collections.unmodifiableList(disabledPrefixes);
    }

    public List<String> getEnabledRegularExpressions() {
        return enabledRegularExpressions == null ?
                Collections.emptyList() : Collections.unmodifiableList(enabledRegularExpressions);
    }

    public void setModEnabled(final boolean modEnabled) {
        this.modEnabled = modEnabled;
    }

    public void setChatEnabled(final boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public void setEnabledPrefixes(final List<String> enabledPrefixes) {
        this.enabledPrefixes = enabledPrefixes;
    }

    public void setDisabledPrefixes(final List<String> disabledPrefixes) {
        this.disabledPrefixes = disabledPrefixes;
    }

    public void setEnabledRegularExpressions(final List<String> enabledRegularExpressions) {
        this.enabledRegularExpressions = enabledRegularExpressions;
    }
}
