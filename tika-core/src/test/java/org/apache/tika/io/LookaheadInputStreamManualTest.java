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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.junit.jupiter.api.Test;

/**
 * IFT3913 tache 2 : tests ecrits a la main pour tuer les mutants de
 * {@link LookaheadInputStream} qui survivent aux tests originaux et aux
 * tests generes par ChatUniTest.
 */
public class LookaheadInputStreamManualTest {

    /**
     * Flux qui ne renvoie jamais plus d'un octet par appel a read(byte[], int, int),
     * comme le ferait un flux reseau lent.
     */
    private static class OneByteAtATimeInputStream extends ByteArrayInputStream {
        OneByteAtATimeInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            // contrat de InputStream.read : IndexOutOfBoundsException si off + len depasse b
            Objects.checkFromIndexSize(off, len, b.length);
            return super.read(b, off, Math.min(len, 1));
        }
    }

    /**
     * Flux qui echoue si on lui demande de lire zero octet : une telle lecture
     * est inutile et signale un depassement de la borne du tampon.
     */
    private static class NoZeroLengthReadInputStream extends ByteArrayInputStream {
        NoZeroLengthReadInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            if (len == 0) {
                throw new AssertionError("lecture de longueur 0 inattendue");
            }
            return super.read(b, off, len);
        }
    }

    @Test
    public void testMarkIsSetAtCurrentPositionOfUnderlyingStream() throws IOException {
        InputStream stream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        assertEquals('a', stream.read());
        InputStream lookahead = new LookaheadInputStream(stream, 1);
        assertEquals('b', lookahead.read());
        lookahead.close();
        assertEquals('b', stream.read());
    }

    @Test
    public void testFillsBufferFromStreamReturningOneByteAtATime() throws IOException {
        InputStream stream = new OneByteAtATimeInputStream(new byte[]{'a', 'b', 'c', 'd'});
        InputStream lookahead = new LookaheadInputStream(stream, 3);
        assertEquals('a', lookahead.read());
        assertEquals('b', lookahead.read());
        assertEquals('c', lookahead.read());
        assertEquals(-1, lookahead.read());
        lookahead.close();
        assertEquals('a', stream.read());
    }

    @Test
    public void testNoReadFromStreamOnceBufferIsFull() throws IOException {
        InputStream stream = new NoZeroLengthReadInputStream(new byte[]{'a', 'b', 'c'});
        InputStream lookahead = new LookaheadInputStream(stream, 2);
        assertEquals('a', lookahead.read());
        assertEquals('b', lookahead.read());
        assertEquals(-1, lookahead.read());
        assertEquals(-1, lookahead.read());
    }

    @Test
    public void testUnderlyingStreamIsResetAsSoonAsEndOfStreamIsReached() throws IOException {
        InputStream stream = new ByteArrayInputStream(new byte[]{'a', 'b'});
        InputStream lookahead = new LookaheadInputStream(stream, 10);
        assertEquals('a', lookahead.read());
        assertEquals('b', lookahead.read());
        assertEquals(-1, lookahead.read());
        // sans appeler lookahead.close(), le flux sous-jacent doit deja etre rembobine
        assertEquals('a', stream.read());
    }

    @Test
    public void testReadArrayCopiesAtMostBufferedBytesAtOffset() throws IOException {
        InputStream stream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c', 'd', 'e'});
        InputStream lookahead = new LookaheadInputStream(stream, 3);
        byte[] b = new byte[]{'x', 'x', 'x', 'x', 'x', 'x'};
        assertEquals(2, lookahead.read(b, 1, 2));
        assertArrayEquals(new byte[]{'x', 'a', 'b', 'x', 'x', 'x'}, b);
        assertEquals(1, lookahead.read(b, 0, 10));
        assertEquals('c', b[0]);
        assertEquals(-1, lookahead.read(b, 0, 10));
    }

    @Test
    public void testMarkSupported() {
        assertTrue(new LookaheadInputStream(null, 1).markSupported());
    }
}
