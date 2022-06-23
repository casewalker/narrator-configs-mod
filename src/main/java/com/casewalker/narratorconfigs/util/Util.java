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
package com.casewalker.narratorconfigs.util;

import net.minecraft.client.option.NarratorMode;

/**
 * Utilities and constants for this mod.
 *
 * @author Case Walker
 */
public class Util {

    /**
     * Use a hard-coded ID for the CUSTOM_NARRATION mode defined in {@link
     * com.casewalker.narratorconfigs.mixin.NarratorModeMixin}.
     *
     * The ID is set to be 29 so that if this mod is all alone, the new NarratorMode will be the 5th member of the enum
     * and {@link NarratorMode#byId(int)} should return the 5th element (index 4) when given ID 29. Or if somehow the
     * Narrates Chat Mod is also present in the environment, the new NarratorMode will be the 6th member of the enum and
     * the ID should resolve to the 6th element (index 5) in that scenario.
     */
    public static final int CUSTOM_NARRATION_ID = 29;

    /**
     * Get the CUSTOM_NARRATION mode defined in {@link com.casewalker.narratorconfigs.mixin.NarratorModeMixin}.
     *
     * @return The CUSTOM_NARRATION narrator mode
     */
    public static NarratorMode customNarration() {
        return NarratorMode.byId(CUSTOM_NARRATION_ID);
    }
}
