package org.apache.tika.io;

import java.io.IOException;
import java.io.InputStream;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

public class LookaheadInputStream_markSupported_6_0_Test {

    private LookaheadInputStream lookaheadInputStream;

    @BeforeEach
    public void setUp() {
        InputStream mockInputStream = new InputStream() {

            @Override
            public int read() throws IOException {
                return 0;
            }
        };
        lookaheadInputStream = new LookaheadInputStream(mockInputStream, 10);
    }

    @Test
    public void testMarkSupported() {
        assertTrue(lookaheadInputStream.markSupported());
    }
}
