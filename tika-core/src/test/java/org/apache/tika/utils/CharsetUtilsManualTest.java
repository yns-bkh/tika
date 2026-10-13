/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IFT3913 tache 2 : tests ecrits a la main pour tuer les mutants de
 * {@link CharsetUtils} qui survivent aux tests originaux et aux tests
 * generes par ChatUniTest.
 */
public class CharsetUtilsManualTest {

    private static final String FAKE_ICU_NAME = "x-fake-icu";

    /**
     * Remplace ICU4J (absent du classpath de tika-core) : memes signatures que
     * CharsetICU.isSupported(String) et CharsetICU.forNameICU(String).
     */
    public static class FakeIcu {
        public static boolean isSupported(String name) {
            if ("boom".equals(name)) {
                throw new IllegalStateException("panne simulee de ICU");
            }
            return FAKE_ICU_NAME.equals(name);
        }

        public static Charset forNameICU(String name) {
            return FAKE_ICU_NAME.equals(name) ? StandardCharsets.UTF_8 : null;
        }
    }

    private Object savedIsSupportedICU;
    private Object savedGetCharsetICU;

    @BeforeEach
    public void saveIcuFields() throws Exception {
        savedIsSupportedICU = icuField("isSupportedICU").get(null);
        savedGetCharsetICU = icuField("getCharsetICU").get(null);
    }

    @AfterEach
    public void restoreIcuFields() throws Exception {
        icuField("isSupportedICU").set(null, savedIsSupportedICU);
        icuField("getCharsetICU").set(null, savedGetCharsetICU);
    }

    private static Field icuField(String name) throws NoSuchFieldException {
        Field f = CharsetUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void installFakeIcu() throws Exception {
        icuField("isSupportedICU").set(null, FakeIcu.class.getMethod("isSupported", String.class));
        icuField("getCharsetICU").set(null, FakeIcu.class.getMethod("forNameICU", String.class));
    }

    @Test
    public void testIcuIsSupportedTakesPrecedenceOverJdk() throws Exception {
        assertFalse(CharsetUtils.isSupported(FAKE_ICU_NAME));
        installFakeIcu();
        assertTrue(CharsetUtils.isSupported(FAKE_ICU_NAME));
    }

    @Test
    public void testIcuNotSupportedFallsBackToJdk() throws Exception {
        installFakeIcu();
        assertTrue(CharsetUtils.isSupported("UTF-8"));
        assertFalse(CharsetUtils.isSupported("bogus"));
    }

    @Test
    public void testIcuFailureMakesCharsetUnsupported() throws Exception {
        installFakeIcu();
        assertFalse(CharsetUtils.isSupported("boom"));
    }

    @Test
    public void testIcuForNameUsedWhenNotACommonCharset() throws Exception {
        installFakeIcu();
        assertEquals(StandardCharsets.UTF_8, CharsetUtils.forName(FAKE_ICU_NAME));
    }

    @Test
    public void testIcuReturningNullFallsBackToJdk() throws Exception {
        installFakeIcu();
        assertEquals(StandardCharsets.US_ASCII, CharsetUtils.forName("ascii"));
        assertThrows(UnsupportedCharsetException.class, () -> CharsetUtils.forName("bogus"));
    }

    @Test
    public void testNoneIsRejectedAsIllegalName() {
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("none"));
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("NONE"));
    }

    @Test
    public void testNoIsRejectedAsIllegalName() {
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("no"));
    }

    @Test
    public void testUnknownNameIsUnsupportedNotIllegal() {
        assertThrows(UnsupportedCharsetException.class, () -> CharsetUtils.forName("nothing"));
    }

    @Test
    public void testNullNameIsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> CharsetUtils.forName(null));
    }

    @Test
    public void testNameNotMatchingPatternIsIllegal() {
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("   "));
    }

    @Test
    public void testForNameReturnsCharsetObject() {
        assertEquals(StandardCharsets.UTF_8, CharsetUtils.forName("  \"UTF-8\"; foo"));
        assertEquals(StandardCharsets.ISO_8859_1, CharsetUtils.forName("8859-1"));
    }
}
