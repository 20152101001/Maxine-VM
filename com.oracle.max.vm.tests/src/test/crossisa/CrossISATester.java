/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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
package test.crossisa;

import java.io.*;
import java.math.*;
import java.util.*;

public abstract class CrossISATester {

    private static final   String ENABLE_QEMU    = "test.crossisa.qemu";
    protected static final File   qemuOutput     = new File("qemu_output");
    protected static final File   qemuErrors     = new File("qemu_errors");
    private static final   File   bindOutput     = new File("bind_output");
    protected static final File   gdbOutput      = new File("gdb_output");
    protected static final String gdbInput       = "gdb_input";
    protected static final String gdbInputFPREGS = "gdb_input_fpregs";
    protected static final File   gdbErrors      = new File("gdb_errors");
    protected static final File   gccOutput      = new File("gcc_output");
    protected static final File   gccErrors      = new File("gcc_errors");
    protected static final File   asOutput       = new File("as_output");
    protected static final File   asErrors       = new File("as_errors");
    protected static final File   linkOutput     = new File("link_output");
    protected static final File   linkErrors     = new File("link_errors");

    public static  boolean ENABLE_SIMULATOR = true;
    private static boolean RESET            = false;
    private static boolean DEBUG            = false;

    protected BitsFlag[] bitMasks;
    protected Process    gcc;
    protected Process    assembler;
    protected Process    linker;
    protected Process    qemu;
    protected Process    gdb;
    protected int[]      simulatedIntRegisters;
    protected int[]      expectedIntRegisters;
    protected boolean[]  testIntRegisters;
    protected long[]     simulatedLongRegisters;
    protected long[]     expectedLongRegisters;
    protected boolean[]  testLongRegisters;
    protected float[]    simulatedFloatRegisters;
    protected float[]    expectedFloatRegisters;
    protected boolean[]  testFloatRegisters;
    protected double[]   simulatedDoubleRegisters;
    protected double[]   expectedDoubleRegisters;
    protected boolean[]  testDoubleRegisters;

    protected CrossISATester() {
    }

    public static void setBitMask(BitsFlag[] bitmasks, int i, BitsFlag mask) {
        bitmasks[i] = mask;
    }

    public static void setAllBitMasks(BitsFlag[] bitmasks, BitsFlag mask) {
        for (int i = 0; i < bitmasks.length; i++) {
            setBitMask(bitmasks, i, mask);
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public int[] getSimulatedIntRegisters() {
        return simulatedIntRegisters;
    }

    public long[] getSimulatedLongRegisters() {
        return simulatedLongRegisters;
    }

    public float[] getSimulatedFloatRegisters() {
        return simulatedFloatRegisters;
    }

    public double[] getSimulatedDoubleRegisters() {
        return simulatedDoubleRegisters;
    }

    public boolean validateIntRegisters() {
        boolean valid   = true;

        assert expectedIntRegisters != null;
        assert simulatedIntRegisters != null;
        assert testIntRegisters != null;
        for (int i = 0; i < simulatedIntRegisters.length; i++) {
            log(i + " sim: " + simulatedIntRegisters[i] + " exp: " + expectedIntRegisters[i] + " test: " + testIntRegisters[i]);
            if (testIntRegisters[i]) {
                final int simulatedRegister = simulatedIntRegisters[i] & (int) bitMasks[i].value();
                final int expectedRegister  = expectedIntRegisters[i] & (int) bitMasks[i].value();
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            for (int i = 0; i < simulatedIntRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedIntRegisters[i] + " exp: " + expectedIntRegisters[i]
                                   + " test: " + testIntRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateLongRegisters() {
        boolean valid   = true;

        assert expectedLongRegisters != null;
        assert simulatedLongRegisters != null;
        assert testLongRegisters != null;
        for (int i = 0; i < simulatedLongRegisters.length; i++) {
            log(i + " sim: " + simulatedLongRegisters[i] + " exp: " + expectedLongRegisters[i] + " test: " + testLongRegisters[i]);
            if (testLongRegisters[i]) {
                final long simulatedRegister = simulatedLongRegisters[i] & bitMasks[i].value();
                final long expectedRegister  = expectedLongRegisters[i] & bitMasks[i].value();
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            for (int i = 0; i < simulatedLongRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedLongRegisters[i] + " exp: " + expectedLongRegisters[i]
                                   + " test: " + testLongRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateFloatRegisters() {
        boolean valid   = true;

        assert expectedFloatRegisters != null;
        assert simulatedFloatRegisters != null;
        assert testFloatRegisters != null;
        for (int i = 0; i < simulatedFloatRegisters.length; i++) {
            log(i + " sim: " + simulatedFloatRegisters[i] + " exp: " + expectedFloatRegisters[i] + " test: " + testFloatRegisters[i]);
            if (testFloatRegisters[i]) {
                final float simulatedRegister = simulatedFloatRegisters[i];
                final float expectedRegister  = expectedFloatRegisters[i];
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            for (int i = 0; i < simulatedFloatRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedFloatRegisters[i] + " exp: " + expectedFloatRegisters[i]
                                   + " test: " + testFloatRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateDoubleRegisters() {
        boolean valid   = true;

        assert expectedDoubleRegisters != null;
        assert simulatedDoubleRegisters != null;
        assert testDoubleRegisters != null;
        for (int i = 0; i < simulatedDoubleRegisters.length; i++) {
            log(i + " sim: " + simulatedDoubleRegisters[i] + " exp: " + expectedDoubleRegisters[i] + " test: " + testDoubleRegisters[i]);
            if (testDoubleRegisters[i]) {
                final double simulatedRegister = simulatedDoubleRegisters[i];
                final double expectedRegister  = expectedDoubleRegisters[i];
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            for (int i = 0; i < simulatedDoubleRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedDoubleRegisters[i] + " exp: " + expectedDoubleRegisters[i]
                                   + " test: " + testDoubleRegisters[i]);
            }
        }

        return valid;
    }

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    protected Process runBlocking(ProcessBuilder processBuilder) {
        Process process = null;
        try {
            process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return process;
    }

    protected void terminateProcess(Process process) {
        if (process != null) {
            process.destroy();
        }
    }

    public void cleanProcesses() {
        terminateProcess(gcc);
        terminateProcess(assembler);
        terminateProcess(linker);
        terminateProcess(gdb);
        terminateProcess(qemu);
    }

    protected void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void cleanFiles() {
        deleteFile(qemuOutput);
        deleteFile(qemuErrors);
        deleteFile(bindOutput);
        deleteFile(gdbOutput);
        deleteFile(gdbErrors);
        deleteFile(gccOutput);
        deleteFile(gccErrors);
        deleteFile(asOutput);
        deleteFile(asErrors);
        deleteFile(linkOutput);
        deleteFile(linkErrors);
    }

    public void reset() {
        if (RESET) {
            cleanFiles();
        }
        cleanProcesses();
    }

    protected void bindToQemu() throws InterruptedException, IOException {
        do {
            ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:1234");
            bindTest.redirectOutput(bindOutput);
            bindTest.start().waitFor();
            FileInputStream inputStream = new FileInputStream(bindOutput);
            if (inputStream.available() != 0) {
                log("CrossISATester: qemu ready");
                inputStream.close();
                break;
            } else {
                log("CrossISATester: qemu not ready");
                Thread.sleep(500);
            }
        } while (true);
    }

    /**
     * Parses the integer registers (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *   r0             0x43d78  277880
     *   r1             0x40000000        1073741824
     *   r2             0x21     33
     *   r3             0x43d08  277768
     *   r4             0x0      0
     * </pre>
     *
     * @param startRegister The first integer register to parse
     * @param endRegister The last integer register to parse
     * @return An array with the parsed values of the integer registers
     * @throws IOException
     */
    protected void parseIntRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader     reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Integer> parsedValues = new ArrayList<>(32);
        String             line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseIntRegister(line));
        } while ((line = reader.readLine()) != null);
        simulatedIntRegisters = new int[parsedValues.size()];
        for (int i = 0; i < simulatedIntRegisters.length; i++) {
            simulatedIntRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses an integer register (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *   r0             0x43d78  277880
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed integer value of the register
     */
    protected int parseIntRegister(String line) {
        String value = line.split("\\s+")[1];
        assert value.startsWith("0x");
        assert value.length() - 2 <= 8;
        BigInteger tmp = new BigInteger(value.substring(2, value.length()), 16);
        if (tmp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            BigInteger result = BigInteger.valueOf(Integer.MIN_VALUE);
            tmp = result.multiply(BigInteger.valueOf(2)).add(tmp);
            assert tmp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 : "Parsed non int value";
        }
        return tmp.intValue();
    }

    /**
     * Parses the long registers (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     r0             0x43d78  277880
     *     r1             0x40000000        1073741824
     *     r2             0x21     33
     *     r3             0x43d08  277768
     *     r4             0x0      0
     * </pre>
     *
     * @param startRegister The first integer register to parse
     * @param endRegister The last integer register to parse
     * @return An array with the parsed values of the integer registers
     * @throws IOException
     */
    protected void parseLongRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader  reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Long> parsedValues = new ArrayList<>(32);
        String          line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseLongRegister(line));
        } while ((line = reader.readLine()) != null);
        simulatedLongRegisters = new long[parsedValues.size()];
        for (int i = 0; i < simulatedLongRegisters.length; i++) {
            simulatedLongRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a long register (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     r0             0x43d78  277880
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed integer value of the register
     */
    protected long parseLongRegister(String line) {
        String value = line.split("\\s+")[1];
        assert value.startsWith("0x");
        assert value.length() - 2 <= 16;
        BigInteger tmp = new BigInteger(value.substring(2, value.length()), 16);
        if (tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            BigInteger result = BigInteger.valueOf(Long.MIN_VALUE);
            tmp = result.multiply(BigInteger.valueOf(2)).add(tmp);
            assert tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 : "Parsed non long value";
        }
        return tmp.longValue();
    }

    /**
     * Parses the float registers (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     s0  1.55405596e-31  (raw 0x0c49ba5e)
     *     s1  4.47656345  (raw 0x408f4002)
     *     s2  1.55405596e-31  (raw 0x0c49ba5e)
     *     s3  4.47656345  (raw 0x408f4002)
     *     s4  1.55405596e-31  (raw 0x0c49ba5e)
     *     s5  4.47656345  (raw 0x408f4002)
     *     s6  0  (raw 0x00000000)
     * </pre>
     *
     * @param startRegister The first float register to parse
     * @param endRegister The last float register to parse
     * @return An array with the parsed values of the float registers
     * @throws IOException
     */
    protected void parseFloatRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader   reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Float> parsedValues = new ArrayList<>(32);
        String           line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseFloatRegister(line));
        } while ((line = reader.readLine()) != null);
        simulatedFloatRegisters = new float[parsedValues.size()];
        for (int i = 0; i < simulatedFloatRegisters.length; i++) {
            simulatedFloatRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a float register (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     s0  1.55405596e-31  (raw 0x0c49ba5e)
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed float value of the register
     */
    protected float parseFloatRegister(String line) {
        String value = line.split("\\s+")[1];

        if (value.contains("nan")) {
            return Float.NaN;
        } else if (value.equals("inf")) {
            return Float.POSITIVE_INFINITY;
        } else if (value.equals("-inf")) {
            return Float.NEGATIVE_INFINITY;
        }

        return Float.parseFloat(value);
    }

    /**
     * Parses the double registers (64-bit) from the output of the gdb command {@code info all-registers}.  The
     * output is expected to be in the form:
     *
     * <pre>
     *     d0  {u8 = {0x5e, 0xba, 0x49, 0xc, 0x2, 0x40, 0x8f, 0x40}, u16 = {0xba5e, 0xc49, 0x4002, 0x408f}, u32 = {0xc49ba5e, 0x408f4002}, u64 = 0x408f40020c49ba5e, f32 = {0x0, 0x4}, f64 = 0x3e8}
     *     d1  {u8 = {0x5e, 0xba, 0x49, 0xc, 0x2, 0x40, 0x8f, 0x40}, u16 = {0xba5e, 0xc49, 0x4002, 0x408f}, u32 = {0xc49ba5e, 0x408f4002}, u64 = 0x408f40020c49ba5e, f32 = {0x0, 0x4}, f64 = 0x3e8}
     *     d2  {u8 = {0x5e, 0xba, 0x49, 0xc, 0x2, 0x40, 0x8f, 0x40}, u16 = {0xba5e, 0xc49, 0x4002, 0x408f}, u32 = {0xc49ba5e, 0x408f4002}, u64 = 0x408f40020c49ba5e, f32 = {0x0, 0x4}, f64 = 0x3e8}
     *     d3  {u8 = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}, u16 = {0x0, 0x0, 0x0, 0x0}, u32 = {0x0, 0x0}, u64 = 0x0, f32 = {0x0, 0x0}, f64 = 0x0}
     *     d4  {u8 = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}, u16 = {0x0, 0x0, 0x0, 0x0}, u32 = {0x0, 0x0}, u64 = 0x0, f32 = {0x0, 0x0}, f64 = 0x0}
     * </pre>
     *
     * @param startRegister The first double register to parse
     * @param endRegister The last double register to parse
     * @return An array with the parsed values of the double registers
     * @throws IOException
     */
    protected void parseDoubleRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader    reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Double> parsedValues = new ArrayList<>(32);
        String            line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseDoubleRegister(line));
        } while ((line = reader.readLine()) != null);
        simulatedDoubleRegisters = new double[parsedValues.size()];
        for (int i = 0; i < simulatedDoubleRegisters.length; i++) {
            simulatedDoubleRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a double register (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     d0  {u8 = {0x5e, 0xba, 0x49, 0xc, 0x2, 0x40, 0x8f, 0x40}, u16 = {0xba5e, 0xc49, 0x4002, 0x408f}, u32 = {0xc49ba5e, 0x408f4002}, u64 = 0x408f40020c49ba5e, f32 = {0x0, 0x4}, f64 = 0x3e8}
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed double value of the register
     */
    protected double parseDoubleRegister(String line) {
        String value = line.split("\\s+")[23];
        value = value.substring(2, value.length() - 1);
        return Double.longBitsToDouble(hexToLongBits(value));
    }

    /**
     * Decodes a hex string representing a double.
     *
     * @param hex The hex string to decode
     * @return The double decoded from {@code hex}
     */
    private static long hexToLongBits(String hex) {
        assert hex.length() <= 16;
        if (hex.length() == 16) { // Split hex string to allow parsing
            long lsbs = hexToLongBits(hex.substring(8));
            long msbs = hexToLongBits(hex.substring(0, 8));
            return msbs << 32 | lsbs;
        }
        return Long.parseLong(hex, 16);
    }

    protected void initializeQemu() {
        ENABLE_SIMULATOR = Integer.getInteger(ENABLE_QEMU) != null && Integer.getInteger(ENABLE_QEMU) > 0;
    }

    public enum BitsFlag {
        NZCBits(0xe0000000L), NZCVBits(0xf0000000L), Lower16Bits(0x0000ffffL), Upper16Bits(0xffff0000L),
        All32Bits(0xffffffffL), Lower32Bits(0xffffffffL), All64Bits(0xffffffffffffffffL);

        private final long value;

        BitsFlag(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    public void runSimulation(ProcessBuilder gdbProcess, ProcessBuilder qemuProcess) {
        gdbProcess.redirectOutput(gdbOutput);
        gdbProcess.redirectError(gdbErrors);
        qemuProcess.redirectOutput(qemuOutput);
        qemuProcess.redirectError(qemuErrors);
        try {
            qemu = qemuProcess.start();
            while (!qemuOutput.exists()) {
                Thread.sleep(500);
            }
            bindToQemu();
            gdb = runBlocking(gdbProcess);
        } catch (Exception e) {
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
    }

    protected abstract void runSimulation() throws Exception;

    public void link() {
        final ProcessBuilder link = getLinkerProcessBuilder();
        link.redirectOutput(linkOutput);
        link.redirectError(linkErrors);
        linker = runBlocking(link);
    }

    protected abstract ProcessBuilder getLinkerProcessBuilder();

    public void compile() {
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "test.elf");
        final ProcessBuilder compile = getCompilerProcessBuilder();
        compile.redirectOutput(gccOutput);
        compile.redirectError(gccErrors);
        runBlocking(removeFiles);
        gcc = runBlocking(compile);
    }

    protected abstract ProcessBuilder getCompilerProcessBuilder();

    public void assembleStartup() {
        final ProcessBuilder assemble = getAssemblerProcessBuilder();
        assemble.redirectOutput(asOutput);
        assemble.redirectError(asErrors);
        assembler = runBlocking(assemble);
    }

    protected abstract ProcessBuilder getAssemblerProcessBuilder();

    public void run() throws Exception {
        assembleStartup();
        compile();
        link();
        runSimulation();
    }
}
