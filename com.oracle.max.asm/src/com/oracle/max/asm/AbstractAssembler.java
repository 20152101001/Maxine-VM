/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.asm;

import java.io.*;
import java.util.concurrent.atomic.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiArchitecture.*;

/**
 * The platform-independent base class for the assembler.
 */
public abstract class AbstractAssembler {
    public final CiTarget target;
    public final Buffer codeBuffer;
    public static boolean DEBUG_METHODS;
    public static boolean SIMULATE_PLATFORM; // on if we use the FPGA simulation platform
    public static boolean FLOAT_IDIV; // on arm platforms we set this to true when do not have  an integer divide unit
    public static AtomicInteger methodCounter = new AtomicInteger(536870912);

    public AbstractAssembler(CiTarget target) {
        this.target = target;

        if (target.arch.byteOrder == ByteOrder.BigEndian) {
            this.codeBuffer = new Buffer.BigEndian();
        } else {
            this.codeBuffer = new Buffer.LittleEndian();
        }
        initDebugMethods();
    }

    public final void bind(Label l) {
        assert !l.isBound() : "can bind label only once";
        l.bind(codeBuffer.position());
        l.patchInstructions(this);
    }

    protected abstract void patchJumpTarget(int branch, int target);

    protected final void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    protected final void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    protected final void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    protected final void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    public final void offlineAddToBuffer(byte[] b) {
        codeBuffer.offlineCopyBuffer(b);
    }

    private static File file;
    private static final Object fileLock = new Object();
    private static boolean debugEnabled = false;

    public static synchronized void initDebugMethods() {
        if (debugEnabled) {
            return;
        }
        if (AbstractAssembler.DEBUG_METHODS) {
            debugEnabled = true;
            if ((file = new File(getDebugMethodsPath() + "debug_methods")).exists()) {
                file.delete();
            }
            file = new File(getDebugMethodsPath() + "debug_methods");
        }
    }

    public static void writeDebugMethod(String name, int index) throws Exception {
        synchronized (fileLock) {
            try {
                assert AbstractAssembler.DEBUG_METHODS;
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(index + " " + name + "\n");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String getDebugMethodsPath() {
        return System.getenv("MAXINE_HOME") + "/maxine-tester/junit-tests/";

    }
}
