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
public class LookaheadInputStream_read_3_0_Test {

    @Mock
    private InputStream mockStream;

    private LookaheadInputStream lookaheadInputStream;

    @BeforeEach
    public void setUp() {
        lookaheadInputStream = new LookaheadInputStream(mockStream, 10);
    }

    @Test
    public void testReadWithBufferedData() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(5);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(5, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
    }

    @Test
    public void testReadWithNoBufferedData() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(-1, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
    }

    @Test
    public void testReadWithPartialBufferedData() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(3);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(3, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
    }

    @Test
    public void testReadWithException() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Mocked exception"));
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        assertThrows(IOException.class, () -> lookaheadInputStream.read(buffer, off, len));
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
    }

    @Test
    public void testReadWithBufferedDataAndReset() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(5);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(5, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
        lookaheadInputStream.close();
        verify(mockStream, times(1)).reset();
    }

    @Test
    public void testReadWithNoBufferedDataAndReset() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(-1, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
        lookaheadInputStream.close();
        verify(mockStream, times(1)).reset();
    }

    @Test
    public void testReadWithPartialBufferedDataAndReset() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(3);
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        int result = lookaheadInputStream.read(buffer, off, len);
        assertEquals(3, result);
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
        lookaheadInputStream.close();
        verify(mockStream, times(1)).reset();
    }

    @Test
    public void testReadWithExceptionAndReset() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Mocked exception"));
        byte[] buffer = new byte[10];
        int off = 0;
        int len = 5;
        assertThrows(IOException.class, () -> lookaheadInputStream.read(buffer, off, len));
        // CORRECTION 1 : oracle faux, la lecture est faite dans le tampon interne (taille 10), pas dans buffer
        verify(mockStream, times(1)).read(any(byte[].class), eq(0), eq(10));
        lookaheadInputStream.close();
        verify(mockStream, times(1)).reset();
    }
}
