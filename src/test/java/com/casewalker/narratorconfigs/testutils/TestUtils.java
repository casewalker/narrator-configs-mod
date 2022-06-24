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
package com.casewalker.narratorconfigs.testutils;

import com.casewalker.narratorconfigs.mixin.NarratorManagerMixinNCM2;
import com.mojang.text2speech.Narrator;
import net.minecraft.util.Pair;
import org.junit.Assert;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities shared between tests for this mod.
 *
 * @author Case Walker
 */
public class TestUtils {

    /**
     * Concrete implementation for the {@link NarratorManagerMixinNCM2} abstract class.
     */
    public static class NarratorManagerMixinTestImpl extends NarratorManagerMixinNCM2 {
        protected void debugPrintMessage(String var1) {}
        public void setNarrator(Narrator narrator) { Whitebox.setInternalState(this, "narrator", narrator); }
    }

    /**
     * Class to mock the narrator.
     */
    public static class DummyNarrator implements Narrator {
        public boolean active;
        public List<Pair<String, Boolean>> thingsSaid = new ArrayList<>();

        public void say(String msg, boolean interrupt) { thingsSaid.add(new Pair<>(msg, interrupt)); }
        public void clear() {}
        public boolean active() { return active; }
        public void destroy() {}
        public void reset() { thingsSaid.clear(); active = false; }
    }

    /**
     * Invert the assertion because JUnit5 inverts the arguments and it is better.
     */
    public static void assertTrue(boolean b, String s) {
        Assert.assertTrue(s, b);
    }

    /**
     * Invert the assertion because JUnit5 inverts the arguments and it is better.
     */
    public static void assertFalse(boolean b, String s) {
        Assert.assertFalse(s, b);
    }
}
