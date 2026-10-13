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
public class LookaheadInputStream_close_0_0_Test {

    @Mock
    private InputStream stream;

    private LookaheadInputStream lookaheadInputStream;

    @BeforeEach
    public void setUp() {
        lookaheadInputStream = new LookaheadInputStream(stream, 10);
    }

    @Test
    public void testClose() throws IOException {
        lookaheadInputStream.close();
        verify(stream, times(1)).reset();
        // CORRECTION 1 : oracle faux, close() rembobine le flux sous-jacent mais ne le ferme pas
        verify(stream, never()).close();
    }

    @Test
    public void testCloseWithNullStream() throws IOException {
        // CORRECTION 2 : erreur de compilation, reset() est void -> doThrow(...).when(...)
        doThrow(new IOException("Stream is already closed")).when(stream).reset();
        // CORRECTION 3 : l'exception de reset() se propage hors de close()
        assertThrows(IOException.class, () -> lookaheadInputStream.close());
        verify(stream, times(1)).reset();
        // CORRECTION 4 : oracle faux, le flux sous-jacent n'est jamais ferme
        verify(stream, never()).close();
    }
}
