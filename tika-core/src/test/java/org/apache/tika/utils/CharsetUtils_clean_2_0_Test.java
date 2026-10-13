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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.*;
import org.mockito.*;

// Test genere par ChatUniTest (qwen2.5-coder:7b), derniere ronde de reparation, corrige a la main (IFT3913 tache 2)
public class CharsetUtils_clean_2_0_Test {

    private CharsetUtils charsetUtils;

    private Method forNameMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        charsetUtils = new CharsetUtils();
        forNameMethod = CharsetUtils.class.getDeclaredMethod("forName", String.class);
        forNameMethod.setAccessible(true);
    }

    @Test
    public void testCleanWithValidCharset() throws Exception {
        String charsetName = "UTF-8";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertEquals(charsetName, result);
    }

    @Test
    public void testCleanWithInvalidCharset() throws Exception {
        String charsetName = "INVALID";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithNullInput() throws Exception {
        String charsetName = null;
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithWhitespace() throws Exception {
        String charsetName = "  UTF-8  ";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertEquals(charsetName.trim(), result);
    }

    @Test
    public void testCleanWithISO8859Pattern() throws Exception {
        String charsetName = "ISO 8859-1";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        // CORRECTION 3 : oracle faux. Malgre le commentaire "Handle iso 8859-x" du code, CHARSET_NAME_PATTERN coupe le nom
        // au premier espace : "ISO 8859-1" devient "ISO", qui n'existe pas -> clean renvoie null
        assertNull(result);
    }

    @Test
    public void testCleanWithCPNamePattern() throws Exception {
        String charsetName = "cp-1252";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        // CORRECTION 4 : oracle faux, clean renvoie le nom canonique du charset (cp1252 est un alias de windows-1252)
        assertEquals("windows-1252", result);
    }

    @Test
    public void testCleanWithWINNamePattern() throws Exception {
        String charsetName = "win-1252";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertEquals("windows-1252", result);
    }

    @Test
    public void testCleanWithIgnoreCase() throws Exception {
        String charsetName = "utf-8";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        // CORRECTION 5 : oracle faux, le nom canonique est en majuscules
        assertEquals("UTF-8", result);
    }

    @Test
    public void testCleanWithNone() throws Exception {
        String charsetName = "none";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithNo() throws Exception {
        String charsetName = "no";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithICU() throws Exception {
        String charsetName = "UTF-8";
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        // CORRECTION 2 : getCharsetICU est un champ, pas une methode (NoSuchMethodException) -> reflexion supprimee
        // CORRECTION 1 : stub Mockito impossible sur un vrai objet Method (MissingMethodInvocationException) -> supprime
        String result = charsetUtils.clean(charsetName);
        assertEquals(charsetName, result);
    }
}
