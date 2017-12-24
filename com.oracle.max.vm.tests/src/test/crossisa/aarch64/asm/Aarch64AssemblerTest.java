/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package test.crossisa.aarch64.asm;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;

import test.crossisa.*;

public class Aarch64AssemblerTest extends MaxTestCase {

    private static final int VARIANT_32 = 32;
    private static final int VARIANT_64 = 64;

    private Aarch64Assembler asm;
    private Aarch64MacroAssembler masm;

    public Aarch64AssemblerTest() {
        CiTarget aarch64 = new CiTarget(new Aarch64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new Aarch64Assembler(aarch64, null);
        masm = new Aarch64MacroAssembler(aarch64, null);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64AssemblerTest.class);
    }

    private static final long[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x7fffffff,
                                                  0xffffffffffffL, 0x7fffffffffffffffL};
    private static final boolean[] testValues = new boolean[MaxineAarch64Tester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static final MaxineAarch64Tester.BitsFlag[] bitmasks =
            new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
    static {
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
    }

    private static void resetIgnoreValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static final long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    private static void setExpectedValue(int cpuRegister, long expectedValue) {
        expectedValues[cpuRegister] = expectedValue;
        testValues[cpuRegister] = true;
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            return;
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        r.reset();
    }

    public void test_b() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov64BitConstant(Aarch64.r0, -1);
        asm.b(24);
        asm.nop(4);                                                          // +16 bytes
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);    // +20 bytes to here (skipped)
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0x123, 0);    // +24 bytes to here (landing)
        expectedValues[0] = -1;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_zero() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov(Aarch64.r0, 0xFF);
        masm.mov(64, Aarch64.r0, Aarch64.zr);
        expectedValues[0] = 0;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }


//    public void test_adr_adrp() throws Exception {
//        initialiseExpectedValues();
//        setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//        resetIgnoreValues();
//        masm.codeBuffer.reset();
//
//        masm.adrp(Aarch64.r0, 0);
//        masm.adrp(Aarch64.r1, 4096);
//        masm.adrp(Aarch64.r2, 8192);
//        masm.adr(Aarch64.r3, 0);
//        masm.mov(Aarch64.r4, 0xfffL);
//
//        long[] reg = generate(expectedValues, testValues, bitmasks,
//                masm.codeBuffer);
//
//        for (int i = 0; i < 5; i++) {
//            System.out.println("REG-" + i + ": " + reg[i]);
//        }
//        assert (reg[3] & ~reg[4]) == reg[0];
//    }

    public void test_mov64() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov64BitConstant(Aarch64.r0, 10);
        masm.mov64BitConstant(Aarch64.r1, -10);
        masm.mov64BitConstant(Aarch64.r2, -12345678987654321L);
        masm.mov64BitConstant(Aarch64.r3, Long.MAX_VALUE);
        masm.mov64BitConstant(Aarch64.r4, Long.MIN_VALUE);

        expectedValues[0] = 10;
        expectedValues[1] = -10;
        expectedValues[2] = -12345678987654321L;
        expectedValues[3] = Long.MAX_VALUE;
        expectedValues[4] = Long.MIN_VALUE;


        testValues[0] = true;
        testValues[1] = true;
        testValues[2] = true;
        testValues[3] = true;
        testValues[4] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_mov() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov(Aarch64.r0, 10);
        masm.mov(Aarch64.r1, -10);

        expectedValues[0] = 10;
        expectedValues[1] = -10;

        testValues[0] = true;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_mov2() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov(Aarch64.r0, Integer.MAX_VALUE);
        masm.mov(Aarch64.r1, Integer.MIN_VALUE);
        masm.mov(Aarch64.r2, Long.MAX_VALUE);
        masm.mov(Aarch64.r3, Long.MIN_VALUE);

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = Long.MAX_VALUE;
        expectedValues[3] = Long.MIN_VALUE;

        testValues[0] = true;
        testValues[1] = true;
        testValues[2] = true;
        testValues[3] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_add_register() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.r0, 10);
        masm.mov64BitConstant(Aarch64.r1, -1);
        masm.mov64BitConstant(Aarch64.r2, 1);
        masm.mov64BitConstant(Aarch64.r3, Long.MAX_VALUE);
        masm.mov64BitConstant(Aarch64.r4, Long.MIN_VALUE);

        masm.add(64, Aarch64.r0, Aarch64.r0, Aarch64.r1);
        masm.add(64, Aarch64.r3, Aarch64.r3, Aarch64.r2);
        masm.add(64, Aarch64.r4, Aarch64.r4, Aarch64.r1);
        masm.add(64, Aarch64.r5, Aarch64.r4, Aarch64.r2);
        masm.add(64, Aarch64.r6, Aarch64.r3, Aarch64.r1);

        expectedValues[0] = 9;
        expectedValues[3] = Long.MIN_VALUE;
        expectedValues[4] = Long.MAX_VALUE;
        expectedValues[5] = Long.MIN_VALUE;
        expectedValues[6] = Long.MAX_VALUE;
        testValues[0] = true;
        testValues[3] = true;
        testValues[4] = true;
        testValues[5] = true;
        testValues[6] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    /**
     * branch.
     */
    public void test_bcond() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();

        masm.mov32BitConstant(Aarch64.r0, 1);
        masm.mov32BitConstant(Aarch64.r1, 1);

        masm.cmp(32, Aarch64.r0, Aarch64.r1);
        masm.b(Aarch64Assembler.ConditionFlag.EQ, 4);
        masm.mov32BitConstant(Aarch64.r3, 66);
        masm.mov32BitConstant(Aarch64.r3, 77);

        expectedValues[3] = 77;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    /**
     * load and store instructions.
     */
    public void test_ldr_str() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -8); // stack address
        asm.str(VARIANT_64,  Aarch64.cpuRegisters[0], address);         // store value to stack
        asm.ldr(VARIANT_64, Aarch64.cpuRegisters[1], address);          // load value from stack
        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_add_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ADD Xi, Xi, valueof(Xi) ; Xi's value should double
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert expectedValues[i] < Integer.MAX_VALUE;
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // SUB Xi, Xi, valueof(Xi) ; Xi should then be 0
        for (int i = 0; i < 10; i++) {
            // expected values must not be larger than an integer in order to be converted to an int correctly
            assert expectedValues[i] < Integer.MAX_VALUE;
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], (int) expectedValues[i]);
            expectedValues[i] = 0; //expectedValues[i] -= expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_and_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // AND Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] &= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_eor_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // EOR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            asm.eor(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] ^= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_orr_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // ORR Xi, Xi, 0x1
        for (int i = 0; i < 10; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], (int) expectedValues[i], 0);
            asm.orr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 0x1);
            expectedValues[i] |= 0x1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_movz_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movz(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = (long) 1 << 32;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_movn_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        // MOVZ Xi, 0x1, LSL #0x10
        for (int i = 0; i < 1; i++) {
            asm.movn(VARIANT_64, Aarch64.cpuRegisters[i], 0x1, 32);
            expectedValues[i] = ~((long) 1 << 32);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_bfm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.bfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111L >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_ubfm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);

        asm.ubfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        expectedValues[0] = 0b111111L >>> 3;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sbfm() throws Exception {
        int mask;
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0b11110000, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0, 0);

        // Signed
        asm.sbfm(VARIANT_64, Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[10], 3, 5);
        mask = 1 << (5 - 1);
        expectedValues[0] = ((0b11110000 >> 3) ^ mask) - mask;
        testValues[0] = true;
        // Unsigned
        asm.sbfm(VARIANT_64, Aarch64.cpuRegisters[1], Aarch64.cpuRegisters[10], 4, 8);
        mask = 1 << (8 - 1);
        expectedValues[1] = ((0b11110000 >> 4) ^ mask) - mask;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (shifted register) (5.5.1). *******/

    public void test_add_shift_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 1, 0);
        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSL, 2);
            expectedValues[i] = 5;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_shift_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0b1000, 0);
        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSR, 3);
            expectedValues[i] = 0b1000 - (0b1000 >>> 3);
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Arithmetic (extended register) (5.5.2). *******/

    public void test_add_ext_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 1, 0);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.add(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ExtendType.UXTB, 3);
            expectedValues[i] = 1 + 0b11111111000L;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_sub_ext_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0b11111111000, 0);
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        for (int i = 0; i < 1; i++) {
            asm.sub(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ExtendType.UXTB, 3);
            expectedValues[i] = 0;
            testValues[i] = true;
        }

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /******* Logical (shifted register) (5.5.3). *******/

    public void test_and_shift_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[0], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0xf, 0);

        for (int i = 0; i < 1; i++) {
            asm.and(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10], Aarch64Assembler.ShiftType.LSL, 4);
            expectedValues[i] = 0b1111L << 4;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    /* Variable Shift (5.5.4) */
    public void test_asr() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], 0b1010101111L);
            asm.mov32BitConstant(Aarch64.cpuRegisters[10], i);
            asm.asr(VARIANT_64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[10]);
            expectedValues[i] = 0b1010101111L >> i;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Bit Operations (5.5.5) */
    public void test_cls() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.movn(VARIANT_64, Aarch64.cpuRegisters[30], 0x0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 0x0, 0);

        asm.cls(VARIANT_64, Aarch64.cpuRegisters[30], Aarch64.cpuRegisters[10]);
        expectedValues[30] = 63;
        testValues[30] = true;
        expectedValues[10] = 0;
        testValues[10] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /* Integer Multiply/Divide (5.6). */

   /* Floating-point Move (register) (5.7.2) */

    public void test_float0() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 11, 0);

        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.fmovCpu2Fpu(VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[0]);
        asm.fmovFpu2Cpu(VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[1]);

        expectedValues[2] = 10;
        testValues[2] = true;
        expectedValues[3] = 11;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

    }

    /**
     * test: fadd, fsub, fmul, fdiv, scvtf, fcvtzs.
     */
    public void test_float1() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 10, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fadd(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fmul(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[5], Aarch64.fpuRegisters[5]);

        expectedValues[0] = 10;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 120;         // fadd
        testValues[2] = true;
        expectedValues[3] = 100;         // fsub
        testValues[3] = true;
        expectedValues[4] = 10 * 110;    // fmul
        testValues[4] = true;
        expectedValues[5] = 110 / 10;    // fdiv
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * frintz, fabs, fneg, fsqrt.
     */
    public void test_float2() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fdiv(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[0]);
        asm.frintz(VARIANT_64, Aarch64.fpuRegisters[2], Aarch64.fpuRegisters[2]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[2], Aarch64.fpuRegisters[2]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fabs(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);

        asm.fsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1]);
        asm.fneg(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[4]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);

        asm.fsqrt(VARIANT_64, Aarch64.fpuRegisters[5], Aarch64.fpuRegisters[0]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[5], Aarch64.fpuRegisters[5]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;
        expectedValues[2] = 1;         // frintz
        testValues[2] = true;
        expectedValues[3] = 10;        // fabs
        testValues[3] = true;
        expectedValues[4] = 10;        // fneg
        testValues[4] = true;
        expectedValues[5] = 10;        // fsqrt
        testValues[5] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * fmadd, fmsub.
     */
    public void test_float3() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 100, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 110, 0);

        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[1], Aarch64.cpuRegisters[1]);

        asm.fmadd(VARIANT_64, Aarch64.fpuRegisters[3], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);
        asm.fmsub(VARIANT_64, Aarch64.fpuRegisters[4], Aarch64.fpuRegisters[0], Aarch64.fpuRegisters[1], Aarch64.fpuRegisters[1]);

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[3], Aarch64.fpuRegisters[3]);
        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[4], Aarch64.fpuRegisters[4]);

        expectedValues[0] = 100;
        testValues[0] = true;
        expectedValues[1] = 110;
        testValues[1] = true;

        expectedValues[3] = 110 + 100 * 110;        // fmadd
        testValues[3] = true;
        expectedValues[4] = 110 - 100 * 110;        // fmsub
        testValues[4] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * fstr, fldr.
     */
    public void test_float4() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0x123, 0);        // value to be stored to stack
        asm.scvtf(VARIANT_64, VARIANT_64, Aarch64.fpuRegisters[0], Aarch64.cpuRegisters[0]);

        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -8); // stack address

        asm.fstr(VARIANT_64, Aarch64.fpuRegisters[0], address);         // store value to stack
        asm.fldr(VARIANT_64, Aarch64.fpuRegisters[1], address);          // load value from stack

        asm.fcvtzs(VARIANT_64, VARIANT_64, Aarch64.cpuRegisters[1], Aarch64.fpuRegisters[1]);

        expectedValues[0] = 0x123;
        testValues[0] = true;
        expectedValues[1] = 0x123;
        testValues[1] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs.
     */
    public void test_mrs() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[10], 1, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[11], 1, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[12], 2, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[13], 3, 0);

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[0], 0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0, 0);
        asm.movz(VARIANT_64, Aarch64.cpuRegisters[2], 0, 0);

        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[10], Aarch64.cpuRegisters[11], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[1], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[13], Aarch64.cpuRegisters[12], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[2], Aarch64Assembler.SystemRegister.NZCV);

        asm.subs(VARIANT_64, Aarch64.cpuRegisters[15], Aarch64.cpuRegisters[12], Aarch64.cpuRegisters[13], Aarch64Assembler.ShiftType.LSL, 0);
        asm.mrs(Aarch64.cpuRegisters[3], Aarch64Assembler.SystemRegister.NZCV);

        expectedValues[0] = 0b0110L << 28;
        testValues[0] = true;
        expectedValues[1] = 0b0110L << 28;
        testValues[1] = true;
        expectedValues[2] = 0b0010L << 28;
        testValues[2] = true;
        expectedValues[3] = 0b1000L << 28;
        testValues[3] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs_reg.
     */
    public void test_mrs_reg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.movz(VARIANT_64, Aarch64.cpuRegisters[1], 0b1111, 0);
        asm.msr(Aarch64Assembler.SystemRegister.SPSR_EL1, Aarch64.cpuRegisters[1]);
        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.SPSR_EL1);

        expectedValues[0] = 0b1111;
        testValues[0] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    /**
     * mrs_imm.
     */
    public void test_mrs_imm() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        asm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFClr, 0b1111);
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_DAIFSet, 0b1001);
        asm.mrs(Aarch64.cpuRegisters[0], Aarch64Assembler.SystemRegister.DAIF);

        // if dst == PSTATEField_SP, then the first 3 digits of the operand is ignored, which means only the last bit is used to set SPSel.
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0001);
        asm.mrs(Aarch64.cpuRegisters[1], Aarch64Assembler.SystemRegister.SPSel);
        asm.msr(Aarch64Assembler.PStateField.PSTATEField_SP, 0b0000);
        asm.mrs(Aarch64.cpuRegisters[2], Aarch64Assembler.SystemRegister.SPSel);

        expectedValues[0] = 0b1001 << 6;
        testValues[0] = true;
        expectedValues[1] = 0b1;
        testValues[1] = true;
        expectedValues[2] = 0;
        testValues[2] = true;

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

//    public void todo_Ldrb() throws Exception {
//        int[] testval = { 0x03020100, 0xffedcba9};
//        int mask = 0xff;
//        initialiseExpectedValues();
//        setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//        resetIgnoreValues();
//        asm.codeBuffer.reset();
//        // load r0 and r1 with sensible values for ignoring the loading of bytes.
//        asm.mov32BitConstant(Aarch64.cpuRegisters[0], testval[0]);
//        asm.mov32BitConstant(Aarch64.cpuRegisters[1], testval[1]);
//        asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//        for (int i = 0; i < 8; i++) {
//            // stackpointer advanced by 8
//            asm.ldrb(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], i);
//            testValues[i] = true;
//            if (i < 4) {
//                expectedValues[i] = testval[0] & (mask << (8 * (i % 4)));
//            } else {
//                expectedValues[i] = testval[1] & (mask << (8 * (i % 4)));
//            }
//
//            expectedValues[i] = expectedValues[i] >> 8 * (i % 4);
//            if (expectedValues[i] < 0) {
//                expectedValues[i] = 0x100 + expectedValues[i];
//            }
//            // Bytes do not have a sign! So we need to make sure the expectedValues are
//            // not affected by sign extension side effects when we take the MSByte of
//            // an integer.
//       }
//
//       generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//      }
//

    private static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }

    public void test_mov32BitConstant() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        int[] values = new int[10];
        values[0] = 0;
        values[1] = -1;
        values[2] = Integer.MIN_VALUE;
        values[3] = Integer.MAX_VALUE;
        values[4] = Integer.MIN_VALUE + 5;
        values[5] = Integer.MAX_VALUE - 5;

        for (int i = 0; i < values.length; i++) {
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], values[i]);

            expectedValues[i] = getUnsignedInt(values[i]);
            testValues[i] = true;
        }

        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_mov64BitConstant() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();

        long[] values = new long[10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = (long) Integer.MIN_VALUE;
        values[3] = (long) Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE - 5;
        values[8] = ((long) Integer.MIN_VALUE) + 5L;
        values[9] = ((long) Integer.MAX_VALUE) - 5L;

        for (long value : values) {
            asm.codeBuffer.reset();
            asm.mov64BitConstant(Aarch64.r0, value);

            expectedValues[0] = value;
            testValues[0] = true;

            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);

        }
    }

    public void test_AddConstant() throws Exception {
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        for (long aScratchTestSet : scratchTestSet) {
            initialiseExpectedValues();
            initialiseTestValues();
            asm.codeBuffer.reset();
            for (int srcReg = 0; srcReg < 3; srcReg++) {
                for (int destReg = 0; destReg < 3; destReg++) {
                    testValues[destReg] = true;
                    asm.mov64BitConstant(Aarch64.cpuRegisters[srcReg], aScratchTestSet);
                    asm.add(64, Aarch64.cpuRegisters[destReg], Aarch64.cpuRegisters[srcReg], 5);
                    expectedValues[destReg] = aScratchTestSet + 5;
                }
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void test_VPushPop() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], i); // r1 = 1
            masm.fmov(64, Aarch64.fpuRegisters[i], Aarch64.cpuRegisters[i]); // d1 = r1 = 1
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], -i); // r1 = -1
        }
        masm.fpush(2 | 8); // save d1
        for (int i = 0; i < 5; i++) {
            masm.fmov(64, Aarch64.fpuRegisters[i], Aarch64.cpuRegisters[i]); // d1 = r1 = -1
        }
        masm.fpop(2 | 8); // pop d1 as 1
        for (int i = 0; i < 5; i++) {
            masm.fmov(64, Aarch64.cpuRegisters[i], Aarch64.fpuRegisters[i]); // see if d1 is 1 or -1
        }
        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[1] = 1;
        testValues[1] = true;
        expectedValues[2] = -2;
        testValues[2] = true;
        expectedValues[3] = 3;
        testValues[3] = true;
        expectedValues[4] = -4;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_PushPop() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], i);
        }
        masm.push(1 | 2 | 4 | 8 | 16);

        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], -i);
        }

        masm.pop(1 | 2 | 4 | 8 | 16);

        expectedValues[0] = 0;
        testValues[0] = true;
        expectedValues[1] = 1;
        testValues[1] = true;
        expectedValues[2] = 2;
        testValues[2] = true;
        expectedValues[3] = 3;
        testValues[3] = true;
        expectedValues[4] = 4;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Vdiv() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], Double.doubleToRawLongBits(10));
        masm.mov64BitConstant(Aarch64.cpuRegisters[1], Double.doubleToRawLongBits(24));
        masm.fmov(64, Aarch64.d0, Aarch64.r0);
        masm.fmov(64, Aarch64.d1, Aarch64.r1);
        masm.fdiv(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fcvtzs(64, 64, Aarch64.r2, Aarch64.d2);
        expectedValues[2] = 2;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Vcvt_int2float() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 10);
        masm.mov32BitConstant(Aarch64.r1, 24);
        masm.scvtf(32, 32, Aarch64.d0, Aarch64.r0);
        masm.scvtf(32, 32, Aarch64.d1, Aarch64.r1);
        masm.fmov(32, Aarch64.r0, Aarch64.d0);
        masm.fmov(32, Aarch64.r1, Aarch64.d1);
        expectedValues[0] = Float.floatToRawIntBits(10);
        testValues[0] = true;
        expectedValues[1] = Float.floatToRawIntBits(24);
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Vcvt_int2double() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.r0, 10);
        masm.mov64BitConstant(Aarch64.r1, 24);
        masm.scvtf(64, 64, Aarch64.d0, Aarch64.r0);
        masm.scvtf(64, 64, Aarch64.d1, Aarch64.r1);
        masm.fmov(64, Aarch64.r0, Aarch64.d0);
        masm.fmov(64, Aarch64.r1, Aarch64.d1);
        expectedValues[0] = Double.doubleToRawLongBits(10);
        testValues[0] = true;
        expectedValues[1] = Double.doubleToRawLongBits(24);
        testValues[1] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Vcvt_double2float() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], Double.doubleToRawLongBits(-10));
        masm.mov64BitConstant(Aarch64.cpuRegisters[2], Double.doubleToRawLongBits(-24));
        masm.fmov(64, Aarch64.d0, Aarch64.r0);
        masm.fmov(64, Aarch64.d1, Aarch64.r2);
        masm.fcvt(64, Aarch64.d4, Aarch64.d0);
        masm.fcvt(64, Aarch64.d5, Aarch64.d1);
        masm.fmov(32, Aarch64.r0, Aarch64.d4);
        masm.fmov(32, Aarch64.r2, Aarch64.d5);
        expectedValues[0] = Float.floatToRawIntBits(-10) & 0xffffffffL;
        testValues[0] = true;
        expectedValues[2] = Float.floatToRawIntBits(-24) & 0xffffffffL;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_VAdd() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.r0, 12);
        masm.mov64BitConstant(Aarch64.r1, 10);
        masm.scvtf(64, 64, Aarch64.d0, Aarch64.r0);
        masm.scvtf(64, 64, Aarch64.d1, Aarch64.r1);
        masm.fadd(64, Aarch64.d2, Aarch64.d0, Aarch64.d1);
        masm.fmov(64, Aarch64.r2, Aarch64.d2);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = Double.doubleToRawLongBits(22);
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_VSub() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.r0, 12);
        masm.mov64BitConstant(Aarch64.r1, 10);
        masm.scvtf(64, 64, Aarch64.d0, Aarch64.r0);
        masm.scvtf(64, 64, Aarch64.d1, Aarch64.r1);
        masm.fsub(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fmov(64, Aarch64.r2, Aarch64.d2);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = Double.doubleToRawLongBits(-2);
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_FcvtMul() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], 12);
        masm.mov64BitConstant(Aarch64.cpuRegisters[1], 10);
        masm.scvtf(64, 64, Aarch64.d0, Aarch64.r0);
        masm.scvtf(64, 64, Aarch64.d1, Aarch64.r1);
        masm.fmul(64, Aarch64.d2, Aarch64.d1, Aarch64.d0);
        masm.fcvtzs(64, 64, Aarch64.r2, Aarch64.d2);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 120;
        testValues[2] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_VldrStr() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], 12);
        masm.mov64BitConstant(Aarch64.cpuRegisters[1], 10);
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024 | 2048); // instruction
        masm.fldr(64, Aarch64.d0, Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.r13));
        masm.fmov(64, Aarch64.r2, Aarch64.d0);
        masm.fldr(64, Aarch64.d4, Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.r13));
        masm.fmov(64, Aarch64.r4, Aarch64.d4);
        masm.fstr(64, Aarch64.d4, Aarch64Address.createUnscaledImmediateAddress(Aarch64.r13, -16));
        masm.fstr(64, Aarch64.d0, Aarch64Address.createUnscaledImmediateAddress(Aarch64.r13, -32));
        masm.fldr(64, Aarch64.d10, Aarch64Address.createUnscaledImmediateAddress(Aarch64.r13, -16));
        masm.fldr(64, Aarch64.d31, Aarch64Address.createUnscaledImmediateAddress(Aarch64.r13, -32));
        masm.fmov(64, Aarch64.r6, Aarch64.d10);
        masm.fmov(64, Aarch64.r8, Aarch64.d31);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        expectedValues[6] = 12;
        testValues[6] = true;
        expectedValues[8] = 12;
        testValues[8] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Vldr() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], 12);
        masm.mov64BitConstant(Aarch64.cpuRegisters[1], 10);
        masm.push(1 | 2);
        Aarch64Address address = Aarch64Address.createBaseRegisterOnlyAddress(Aarch64.r13);
        masm.fldr(64, Aarch64.d31, address);
        masm.fmov(64, Aarch64.r2, Aarch64.d31);
        masm.fldr(64, Aarch64.d4, address);
        masm.fmov(64, Aarch64.r4, Aarch64.d4);
        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_MVov() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov64BitConstant(Aarch64.cpuRegisters[0], 12);
        masm.mov64BitConstant(Aarch64.cpuRegisters[1], 10);
        masm.fmov(64, Aarch64.d0, Aarch64.r0);
        masm.fmov(64, Aarch64.r2, Aarch64.d0);
        masm.fmov(64, Aarch64.d5, Aarch64.r0);
        masm.fmov(64, Aarch64.r4, Aarch64.d5);

        expectedValues[0] = 12;
        testValues[0] = true;
        expectedValues[1] = 10;
        testValues[1] = true;
        expectedValues[2] = 12;
        testValues[2] = true;
        expectedValues[4] = 12;
        testValues[4] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }
//
//     //TODO: Fix vmovimm
//     public void broken_vmovimm() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 1, CiKind.Double);
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s0, 0, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r0, Aarch64.s0, null, CiKind.Int, CiKind.Double);
//         asm.vmovImm(Aarch64Assembler.ConditionFlag.Always, Aarch64.s1, -100, CiKind.Double);
//         asm.vmov(Aarch64Assembler.ConditionFlag.Always, Aarch64.r2, Aarch64.s1,null,  CiKind.Int, CiKind.Double);
//         expectedValues[0] = 0;
//         testValues[0] = true;
//         expectedValues[2] = -100;
//         testValues[2] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
//     public void ignore_FloatIngPointExperiments() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], 12);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], 10);
//         asm.codeBuffer.emitInt(0xee000a10);
//         asm.codeBuffer.emitInt(0xee001a90);
//         asm.codeBuffer.emitInt(0xeeb81ac0);
//         asm.codeBuffer.emitInt(0xeef81ae0);
//         asm.codeBuffer.emitInt(0xee210a21);
//         asm.codeBuffer.emitInt(0xeebd0a40);
//         asm.codeBuffer.emitInt(0xee100a10);
//         expectedValues[0] = 120;
//         testValues[0] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
    public void test_SubReg() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            masm.sub(64, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[5 - (i + 1)], Aarch64.cpuRegisters[i]);
            expectedValues[i + 5] = expectedValues[5 - (i + 1)] - expectedValues[i];
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Mov() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.mov(64, Aarch64.cpuRegisters[i + 5], Aarch64.cpuRegisters[i]);
            expectedValues[i + 5] = expectedValues[i];
            testValues[i] = true;
            testValues[i + 5] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Sub() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.sub(64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], i * 2);
            expectedValues[i] = expectedValues[i] - i * 2;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Str() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        asm.mov32BitConstant(Aarch64.cpuRegisters[12], 0);
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp, Aarch64.cpuRegisters[12], false);
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
            asm.str(VARIANT_64, Aarch64.cpuRegisters[i], address);
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], -2 * (expectedValues[i]));
            asm.ldr(VARIANT_64, Aarch64.cpuRegisters[i], address);
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_Ldr() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            testValues[i] = true;
        }
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        Aarch64Address address = Aarch64Address.createRegisterOffsetAddress(Aarch64.sp, Aarch64.cpuRegisters[12], false);
        for (int i = 9; i < -1; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[12], 16 * i);
            masm.ldr(VARIANT_64, Aarch64.cpuRegisters[i], address);
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Decq() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.decq(Aarch64.cpuRegisters[i]);
            expectedValues[i] -= 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Incq() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            masm.incq(Aarch64.cpuRegisters[i]);
            expectedValues[i] += 1;
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_Subq() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            if (i % 2 == 1) {
                asm.subq(Aarch64.cpuRegisters[i], 2 * expectedValues[i]);
                expectedValues[i] -= 2 * expectedValues[i];
            } else {
                asm.subq(Aarch64.cpuRegisters[i], expectedValues[i]);
                expectedValues[i] -= expectedValues[i];
            }
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

    public void test_addq() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        asm.codeBuffer.reset();
        for (int i = 0; i < 10; i++) {
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            asm.addq(Aarch64.cpuRegisters[i], expectedValues[i]);
            expectedValues[i] += expectedValues[i];
            testValues[i] = true;
        }
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }
//
//     public void ignore_Ldrsh() throws Exception {
//         int[] testval = { 0x03020100, 0x8fed9ba9};
//         int mask = 0xffff;
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         // load r0 and r1 with sensible values for ignoring the loading of bytes.
//         asm.mov32BitConstant(Aarch64.cpuRegisters[0], testval[0]);
//         asm.mov32BitConstant(Aarch64.cpuRegisters[1], testval[1]);
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2); // values now lie on the stack
//         // we now try to extract the "signed halfwords"
//         // from the stack and place them into r0..r3
//         for (int i = 0; i < 4; i++) {
//             // in this test we are using the stack register as the base value!
//             asm.ldrshw(Aarch64Assembler.ConditionFlag.Always, 1, 1, 0, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[13], i * 2);
//             testValues[i] = true;
//             if (i < 2) {
//                 expectedValues[i] = testval[0] & (mask << (16 * (i % 2)));
//             } else {
//                 expectedValues[i] = testval[1] & (mask << (16 * (i % 2)));
//             }
//             if ((expectedValues[i] & 0x8000) != 0) {
//                 expectedValues[i] = expectedValues[i] - 0x10000; // sign extension workaround.
//             }
//             expectedValues[i] = expectedValues[i] >> 16 * (i % 2);
//         }
//
//         generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
//     }
//
    public void test_StrdAndLdrd() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        Aarch64Address address = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, -16); // stack address
        for (int i = 0; i < 10; i += 2) {
            asm.codeBuffer.reset();
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
            asm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], expectedValues[i + 1]);
            asm.str(64, Aarch64.cpuRegisters[i], address);
            asm.mov32BitConstant(Aarch64.cpuRegisters[i], 0);
            asm.mov32BitConstant(Aarch64.cpuRegisters[i + 1], 0);
            asm.ldr(64, Aarch64.cpuRegisters[i], address);
            testValues[i] = true;
            testValues[i + 1] = true;
            if (i != 0) {
                testValues[i - 1] = false;
                testValues[i - 2] = false;
            }
            expectedValues[i + 1] = 0;
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

    public void test_PushAndPop() throws Exception {
        int registers = 1;
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        initialiseExpectedValues();
        for (int i = 0; i < 16; i++) {
            if (i % 2 == 0) {
                expectedValues[i] = i;
            } else {
                expectedValues[i] = -i;
            }
            if (i < 13) {
                testValues[i] = true;
            }
        }

        for (int bitmask = 1; bitmask <= 0xfff; bitmask = bitmask | (bitmask + 1), registers++) {
            masm.codeBuffer.reset();
            for (int i = 0; i < 13; i++) { // we are not breaking the stack (r13)
                masm.mov64BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]); // 2 instructions movw, movt
                // all registers initialized.
            }
            masm.push(bitmask); // store all registers referred to
            // by bitmask on the stack
            for (int i = 0; i < 13; i++) {
                masm.add(64, Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i], 1);
            }
            // r0..r12 should now all have +1 more than their previous values stored on the stack
            // restore the same registers that were placed on the stack
            masm.pop(bitmask);
            for (int i = 0; i < 13; i++) {
                if (i < registers) {
                    expectedValues[i] = expectedValues[i];
                } else {
                    expectedValues[i] = expectedValues[i] + 1;
                }
            }
            generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
        }
    }

    public void test_MovRor() throws Exception {
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        final long value = 0x0fab0dad0badcafeL;

        for (int shift = 1; shift < 64; shift += 4) {
            asm.codeBuffer.reset();
            for (int i = 0; i < 4 && shift < 64; i++, shift++) {
                final int index = i * 3;
                asm.mov64BitConstant(Aarch64.cpuRegisters[index], value);
                setExpectedValue(index, value);
                asm.movror(Aarch64.cpuRegisters[index + 1], Aarch64.cpuRegisters[index], shift);
                setExpectedValue(index, Long.rotateRight(value, shift));
                asm.movror(Aarch64.cpuRegisters[index + 2], Aarch64.cpuRegisters[index + 1], 63 - shift);
                setExpectedValue(index, value);
            }
            generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
        }
    }

//     public void ignore_Ldrd() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         asm.codeBuffer.reset();
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             System.out.println(i + " " + expectedLongValues[i]);
//             asm.mov64BitConstant(Aarch64.cpuRegisters[i * 2], Aarch64.cpuRegisters[(i * 2) + 1], expectedLongValues[i]);
//             testValues[i] = true;
//         }
//         asm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8);
//         for (int i = 0; i < expectedLongValues.length * 2; i++) {
//              asm.mov32BitConstant(Aarch64.cpuRegisters[i],0);
//         }
//         for (int i = 0; i < expectedLongValues.length; i++) {
//             asm.movw(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, i * 8);
//             asm.movt(Aarch64Assembler.ConditionFlag.Always, Aarch64.r12, 0);
//             asm.ldrd(ConditionFlag.Always, 0, 0, 0, Aarch64.RSP.asRegister(), Aarch64.cpuRegisters[i * 2], Aarch64.r12);
//         }
//         generateAndTest(expectedLongValues, testValues, bitmasks);
//     }
//
//     public void test_casInt() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         CiRegister cmpReg = Aarch64.r0;
//         CiRegister newReg = Aarch64.r1;
//
//         // r0=10, r1=20, r2=30, r3=40, r4=50
//         for (int i = 1; i < 5; i++) {
//             masm.mov32BitConstant(Aarch64.cpuRegisters[i], (i + 1) * 10);
//         }
//         masm.mov32BitConstant(Aarch64.cpuRegisters[0], 50);
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 20);
//         masm.casIntAsmTest(newReg, cmpReg, addr);
//         expectedValues[1] = 20;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }
//
//     public void test_casLong() throws Exception {
//         initialiseExpectedValues();
//         setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
//         resetIgnoreValues();
//         masm.codeBuffer.reset();
//         CiRegister cmpReg = Aarch64.r0;
//         CiRegister newReg = Aarch64.r2;
//
//         // r0=10, r1=0
//         // r2=30, r3=0
//         // r4=50, r5=0
//         // r6=70, r7=0
//         // r8=90, r9=0
//         for (int i = 2; i < 10; i += 2) {
//             masm.mov64BitConstant(Aarch64.cpuRegisters[i], Aarch64.cpuRegisters[i + 1], (i + 1) * 10);
//         }
//         masm.mov64BitConstant(Aarch64.cpuRegisters[0], Aarch64.cpuRegisters[1], 90);
//         masm.push(Aarch64Assembler.ConditionFlag.Always, 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
//         CiAddress addr = new CiAddress(CiKind.Int, Aarch64.r13.asValue(), 32);
//         masm.casLongAsmTest(newReg, cmpReg, addr);
//         expectedValues[0] = 30;
//         testValues[0] = true;
//         expectedValues[1] = 0;
//         testValues[1] = true;
//         generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
//     }

    public void test_decrementl() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 100);
        masm.push(1);
        Aarch64Address addr = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, 0);
        masm.decrementl(addr, 10);
        masm.pop(1);
        expectedValues[0] = 90;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }

    public void test_incrementl() throws Exception {
        initialiseExpectedValues();
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
        resetIgnoreValues();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 100);
        masm.push(1);
        Aarch64Address addr = Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, 0);
        masm.incrementl(addr, 10);
        masm.pop(1);
        expectedValues[0] = 110;
        testValues[0] = true;
        generateAndTest(expectedValues, testValues, bitmasks, masm.codeBuffer);
    }
}
