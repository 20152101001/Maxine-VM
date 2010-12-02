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
package com.sun.max.vm.cps.target.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;
import static com.sun.max.asm.amd64.AMD64XMMRegister.*;
import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64TargetABIsScheme extends TargetABIsScheme<AMD64GeneralRegister64, AMD64XMMRegister> {

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> nativeRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RBP,
                        RAX, null, null,
                        AMD64XMMRegister.class, XMM0, null);

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> jitRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RBP,
                        RAX, R11, R14,
                        AMD64XMMRegister.class, XMM0, XMM15);

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> optimizedJavaRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RSP,
                        RAX, R11, R14,
                        AMD64XMMRegister.class, XMM0, XMM15);

    private static final List<AMD64GeneralRegister64> integerParameterRegisters = Arrays.asList(RDI, RSI, RDX, RCX, R8, R9);
    private static final List<AMD64XMMRegister> floatingPointParameterRegisters = Arrays.asList(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7);

    private static final int NULL_STACK_BIAS = 0;

    @HOSTED_ONLY
    static TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> createAMD64TargetABI(RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> registerRoleAssignment, CallEntryPoint callEntryPoint) {
        return  new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(registerRoleAssignment, callEntryPoint,
                        integerParameterRegisters, integerParameterRegisters, floatingPointParameterRegisters,
                        false, true, target().stackAlignment, NULL_STACK_BIAS);
    }

    @HOSTED_ONLY
    public AMD64TargetABIsScheme() {
        super(createAMD64TargetABI(nativeRegisterRoleAssignment, CallEntryPoint.C_ENTRY_POINT),
              createAMD64TargetABI(jitRegisterRoleAssignment, CallEntryPoint.JIT_ENTRY_POINT),
              createAMD64TargetABI(optimizedJavaRegisterRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
    }
}