/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.compiler.builtin.*;

/**
 * A machine word interpreted as a linear address.
 *
 * @author Bernd Mathiske
 */
public abstract class Address extends Word {

    protected Address() {
    }

    @INLINE
    public static Address zero() {
        return fromInt(0);
    }

    @INLINE
    public static Address max() {
        return fromLong(-1L);
    }

    /**
     * Creates an Address value from a given int value. Note that unlike {@link #fromInt(int)},
     * the given int value is not sign extended. Also note that on 32-bit platforms, this operation
     * is effectively a no-op.
     *
     * @param value the value to be converted to an Address
     */
    @INLINE
    public static Address fromUnsignedInt(int value) {
        if (Word.isBoxed()) {
            final long longValue = value;
            final long n = longValue & 0xffffffff;
            return new BoxedAddress(n);
        }
        if (Word.width() == WordWidth.BITS_64) {
            final long longValue = value;
            final long n = longValue & 0xffffffff;
            return UnsafeLoophole.longToWord(Address.class, n);
        }
        return UnsafeLoophole.intToWord(Address.class, value);
    }

    /**
     * Creates an Address value from a given int value. Note that unlike {@link #fromUnsignedInt(int)},
     * the given int value is sign extended first. Also note that on 32-bit platforms, this operation
     * is effectively a no-op.
     *
     * @param value the value to be converted to an Address
     */
    @INLINE
    public static Address fromInt(int value) {
        if (Word.isBoxed()) {
            final long n = value;
            return new BoxedAddress(n);
        }
        if (Word.width() == WordWidth.BITS_64) {
            final long n = value;
            return UnsafeLoophole.longToWord(Address.class, n);
        }
        return UnsafeLoophole.intToWord(Address.class, value);
    }

    @INLINE
    public static Address fromLong(long value) {
        if (Word.isBoxed()) {
            return new BoxedAddress(value);
        }
        if (Word.width() == WordWidth.BITS_64) {
            return UnsafeLoophole.longToWord(Address.class, value);
        }
        final int n = (int) value;
        return UnsafeLoophole.intToWord(Address.class, n);
    }

    @Override
    public String toString() {
        return "@" + toHexString();
    }

    public String toString(int radix) {
        final long n = toLong();
        final long low = n & 0xffffffffL;
        final String s = Long.toString(low, radix);
        final long high = n >>> 32;
        if (Word.width() == WordWidth.BITS_32 || high == 0L) {
            return s;
        }
        return Long.toString(high, radix) + s;
    }

    public static Address parse(String s, int radix) {
        Address result = Address.zero();
        for (int i = 0; i < s.length(); i++) {
            result = result.times(radix);
            result = result.plus(Integer.parseInt(String.valueOf(s.charAt(i)), radix));
        }
        return result;
    }

    @INLINE
    public final int toInt() {
        if (Word.isBoxed()) {
            final UnsafeBox box = (UnsafeBox) this;
            return (int) box.nativeWord();
        }
        if (Word.width() == WordWidth.BITS_64) {
            final long n = UnsafeLoophole.wordToLong(this);
            return (int) n;
        }
        return UnsafeLoophole.wordToInt(this);
    }

    @INLINE
    public final long toLong() {
        if (Word.isBoxed()) {
            final UnsafeBox box = (UnsafeBox) this;
            return box.nativeWord();
        }
        if (Word.width() == WordWidth.BITS_64) {
            return UnsafeLoophole.wordToLong(this);
        }
        return 0xffffffffL & UnsafeLoophole.wordToInt(this);
    }

    public final int compareTo(Address other) {
        if (greaterThan(other)) {
            return 1;
        }
        if (lessThan(other)) {
            return -1;
        }
        return 0;
    }

    @INLINE(override = true)
    public final boolean equals(int other) {
        if (Word.isBoxed()) {
            return toLong() == other;
        }
        return fromInt(other) == this;
    }

    @BUILTIN(builtinClass = AddressBuiltin.GreaterThan.class)
    public final boolean greaterThan(Address other) {
        assert Word.isBoxed();
        final long a = toLong();
        final long b = other.toLong();
        if (a < 0 == b < 0) {
            return a > b;
        }
        return a < b;
    }

    @INLINE(override = true)
    public final boolean greaterThan(int other) {
        return greaterThan(fromInt(other));
    }

    @BUILTIN(builtinClass = AddressBuiltin.GreaterEqual.class)
    public final boolean greaterEqual(Address other) {
        assert Word.isBoxed();
        return !other.greaterThan(this);
    }

    @INLINE(override = true)
    public final boolean greaterEqual(int other) {
        return greaterEqual(fromInt(other));
    }

    @BUILTIN(builtinClass = AddressBuiltin.LessThan.class)
    public final boolean lessThan(Address other) {
        assert Word.isBoxed();
        return other.greaterThan(this);
    }

    @INLINE(override = true)
    public final boolean lessThan(int other) {
        return lessThan(fromInt(other));
    }

    @BUILTIN(builtinClass = AddressBuiltin.LessEqual.class)
    public final boolean lessEqual(Address other) {
        assert Word.isBoxed();
        return !greaterThan(other);
    }

    @INLINE(override = true)
    public final boolean lessEqual(int other) {
        return lessEqual(fromInt(other));
    }

    @INLINE(override = true)
    public Address plus(Address addend) {
        return asOffset().plus(addend.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address plus(Offset offset) {
        return asOffset().plus(offset).asAddress();
    }

    @INLINE(override = true)
    public Address plus(int addend) {
        return asOffset().plus(addend).asAddress();
    }

    @INLINE(override = true)
    public Address minus(Address subtrahend) {
        return asOffset().minus(subtrahend.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address minus(Offset offset) {
        return asOffset().minus(offset).asAddress();
    }

    @INLINE(override = true)
    public Address minus(int subtrahend) {
        return asOffset().minus(subtrahend).asAddress();
    }

    @INLINE(override = true)
    public Address times(Address factor) {
        return asOffset().times(factor.asOffset()).asAddress();
    }

    @INLINE(override = true)
    public Address times(int factor) {
        return asOffset().times(factor).asAddress();
    }

    @BUILTIN(builtinClass = AddressBuiltin.DividedByAddress.class)
    protected abstract Address dividedByAddress(Address divisor);

    @INLINE(override = true)
    public Address dividedBy(Address divisor) {
        return dividedByAddress(divisor);
    }

    @BUILTIN(builtinClass = AddressBuiltin.DividedByInt.class)
    protected abstract Address dividedByInt(int divisor);

    @INLINE(override = true)
    public Address dividedBy(int divisor) {
        return dividedByInt(divisor);
    }

    @BUILTIN(builtinClass = AddressBuiltin.RemainderByAddress.class)
    protected abstract Address remainderByAddress(Address divisor);

    @INLINE(override = true)
    public Address remainder(Address divisor) {
        return remainderByAddress(divisor);
    }

    @BUILTIN(builtinClass = AddressBuiltin.RemainderByInt.class)
    protected abstract int remainderByInt(int divisor);

    @INLINE(override = true)
    public final int remainder(int divisor) {
        return remainderByInt(divisor);
    }

    @INLINE(override = true)
    public final boolean isRoundedBy(Address nBytes) {
        return remainder(nBytes).isZero();
    }

    @INLINE(override = true)
    public final boolean isRoundedBy(int nBytes) {
        return remainder(nBytes) == 0;
    }

    @INLINE(override = true)
    public Address roundedUpBy(Address nBytes) {
        if (isRoundedBy(nBytes)) {
            return this;
        }
        return plus(nBytes.minus(remainder(nBytes)));
    }

    @INLINE(override = true)
    public Address roundedUpBy(int nBytes) {
        if (isRoundedBy(nBytes)) {
            return this;
        }
        return plus(nBytes - remainder(nBytes));
    }

    @INLINE(override = true)
    public Address roundedDownBy(int nBytes) {
        return minus(remainder(nBytes));
    }

    @INLINE(override = true)
    public Address aligned() {
        final int n = Platform.target().processorKind().dataModel().alignment().numberOfBytes();
        return plus(n - 1).and(Address.fromInt(n - 1).not());
    }

    @INLINE(override = true)
    public Address aligned(int alignment) {
        return plus(alignment - 1).and(Address.fromInt(alignment - 1).not());
    }

    @INLINE(override = true)
    public boolean isAligned() {
        final int n = Platform.target().processorKind().dataModel().alignment().numberOfBytes();
        return and(n - 1).equals(Address.zero());
    }

    @INLINE(override = true)
    public boolean isAligned(int alignment) {
        return and(alignment - 1).equals(Address.zero());
    }

    @INLINE(override = true)
    public final boolean isBitSet(int index) {
        return (toLong() & (1L << index)) != 0;
    }

    @INLINE(override = true)
    public Address bitSet(int index) {
        return fromLong(toLong() | (1L << index));
    }

    @INLINE(override = true)
    public Address bitCleared(int index) {
        return fromLong(toLong() & ~(1L << index));
    }

    @INLINE(override = true)
    public Address and(Address operand) {
        if (Word.width() == WordWidth.BITS_64) {
            return fromLong(toLong() & operand.toLong());
        }
        return fromInt(toInt() & operand.toInt());
    }

    @INLINE(override = true)
    public Address and(int operand) {
        return and(fromInt(operand));
    }

    @INLINE(override = true)
    public Address or(Address operand) {
        if (Word.width() == WordWidth.BITS_64) {
            return fromLong(toLong() | operand.toLong());
        }
        return fromInt(toInt() | operand.toInt());
    }

    @INLINE(override = true)
    public Address or(int operand) {
        return or(fromInt(operand));
    }

    @INLINE(override = true)
    public Address not() {
        if (Word.width() == WordWidth.BITS_64) {
            return fromLong(~toLong());
        }
        return fromInt(~toInt());
    }

    @INLINE(override = true)
    public Address shiftedLeft(int nBits) {
        if (Word.width() == WordWidth.BITS_64) {
            return fromLong(toLong() << nBits);
        }
        return fromInt(toInt() << nBits);
    }

    @INLINE(override = true)
    public Address unsignedShiftedRight(int nBits) {
        if (Word.width() == WordWidth.BITS_64) {
            return fromLong(toLong() >>> nBits);
        }
        return fromInt(toInt() >>> nBits);
    }

    @INLINE(override = true)
    public final int numberOfEffectiveBits() {
        if (Word.width() == WordWidth.BITS_64) {
            return 64 - Long.numberOfLeadingZeros(toLong());
        }
        return 32 - Integer.numberOfLeadingZeros(toInt());
    }

    public final WordWidth effectiveWidth() {
        final int bit = numberOfEffectiveBits();
        for (WordWidth width : WordWidth.VALUES) {
            if (bit < width.numberOfBits()) {
                return width;
            }
        }
        throw ProgramError.unexpected();
    }
}
