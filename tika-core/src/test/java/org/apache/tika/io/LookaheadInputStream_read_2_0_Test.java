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
package org.apache.tika.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

// Test genere par ChatUniTest (qwen2.5-coder:7b), derniere ronde de reparation, corrige a la main (IFT3913 tache 2)
@ExtendWith(MockitoExtension.class)
// CORRECTION 1 : stubs inutiles (available() n'est jamais appele par LookaheadInputStream) -> mode LENIENT
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class LookaheadInputStream_read_2_0_Test {

    @Mock
    private InputStream mockStream;

    private LookaheadInputStream lookaheadInputStream;

    @BeforeEach
    public void setUp() {
        lookaheadInputStream = new LookaheadInputStream(mockStream, 10);
    }

    @Test
    public void testReadWhenBuffered() throws IOException {
        when(mockStream.available()).thenReturn(1);
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1);
        lookaheadInputStream.read();
        // CORRECTION 2 : oracle faux, le tampon interne est vide au depart donc fill() lit le flux une fois
        verify(mockStream, times(1)).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    public void testReadWhenStreamIsNull() throws IOException {
        LookaheadInputStream lookaheadInputStream = new LookaheadInputStream(null, 10);
        // CORRECTION 3 : oracle faux, un flux null est traite comme un flux vide (pas de NullPointerException)
        assertEquals(-1, lookaheadInputStream.read());
    }

    @Test
    public void testReadWhenStreamThrowsIOException() throws IOException {
        when(mockStream.available()).thenReturn(1);
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
        assertThrows(IOException.class, () -> {
            lookaheadInputStream.read();
        });
    }

    @Test
    public void testReadWhenNotBuffered() throws IOException {
        when(mockStream.available()).thenReturn(0);
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1);
        lookaheadInputStream.read();
        verify(mockStream, times(1)).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    public void testReadWhenStreamReadsZero() throws IOException {
        when(mockStream.available()).thenReturn(0);
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(0);
        assertEquals(-1, lookaheadInputStream.read());
    }

    @Test
    public void testReadWhenStreamReadsNegativeOne() throws IOException {
        when(mockStream.available()).thenReturn(0);
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        lookaheadInputStream.close();
        assertEquals(-1, lookaheadInputStream.read());
    }
}
