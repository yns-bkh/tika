package org.apache.tika.utils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static java.util.Locale.ENGLISH;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
