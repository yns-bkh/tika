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

public class CharsetUtils_isSupported_1_1_Test {

    private CharsetUtils charsetUtils;

    private Method isSupportedICU;

    @BeforeEach
    public void setUp() throws Exception {
        charsetUtils = new CharsetUtils();
        isSupportedICU = Mockito.mock(Method.class);
        when(isSupportedICU.invoke(any(), anyString())).thenReturn(true);
    }

    @Test
    public void testIsSupportedWithSupportedCharset() throws Exception {
        String charsetName = "UTF-8";
        assertTrue(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithUnsupportedCharset() throws Exception {
        String charsetName = "INVALID-CHARSET";
        assertFalse(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithICUSupportedCharset() throws Exception {
        String charsetName = "UTF-8";
        when(isSupportedICU.invoke(null, charsetName)).thenReturn(true);
        assertTrue(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithICUUnsupportedCharset() throws Exception {
        String charsetName = "INVALID-CHARSET";
        when(isSupportedICU.invoke(null, charsetName)).thenReturn(false);
        assertFalse(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithNull() throws Exception {
        String charsetName = null;
        assertFalse(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithEmptyString() throws Exception {
        String charsetName = "";
        assertFalse(charsetUtils.isSupported(charsetName));
    }

    @Test
    public void testIsSupportedWithISOName() throws Exception {
        String charsetName = "ISO-8859-1";
        assertTrue(charsetUtils.isSupported(charsetName));
    }
}
