/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.api.vm;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import java.util.concurrent.CountDownLatch;
import org.junit.After;

@SuppressWarnings("deprecation")
public class EngineSingleThreadedTest {
    PolyglotEngine tvm;
    CountDownLatch readyForUse = new CountDownLatch(1);
    CountDownLatch waitBeforeDispose = new CountDownLatch(1);
    CountDownLatch disposeOK = new CountDownLatch(1);

    @Before
    public void initInDifferentThread() throws InterruptedException {
        final PolyglotEngine.Builder b = PolyglotEngine.newBuilder();
        Thread t = new Thread("Initializer") {
            @Override
            public void run() {
                tvm = b.build();
                readyForUse.countDown();
                try {
                    waitBeforeDispose.await();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
                tvm.dispose();
                disposeOK.countDown();
            }
        };
        t.start();
        readyForUse.await();
    }

    @After
    public void orderDispose() throws InterruptedException {
        waitBeforeDispose.countDown();
        disposeOK.await();
    }

    @Test(expected = IllegalStateException.class)
    public void evalURI() throws IOException {
        tvm.eval(Source.fromURL(new File(".").toURI().toURL(), "wrong.test"));
    }

    @Test(expected = IllegalStateException.class)
    public void evalString() throws IOException {
        tvm.eval(Source.fromText("1 + 1", "wrong.test").withMimeType("text/javascript"));
    }

    @Test(expected = IllegalStateException.class)
    public void evalReader() throws IOException {
        try (StringReader sr = new StringReader("1 + 1")) {
            tvm.eval(Source.fromReader(sr, "wrong.test").withMimeType("text/javascript"));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void evalSource() throws IOException {
        tvm.eval(Source.fromText("", "Empty"));
    }

    @Test(expected = IllegalStateException.class)
    public void findGlobalSymbol() {
        tvm.findGlobalSymbol("doesNotExists");
    }

    @Test(expected = IllegalStateException.class)
    public void dispose() {
        tvm.dispose();
    }
}
