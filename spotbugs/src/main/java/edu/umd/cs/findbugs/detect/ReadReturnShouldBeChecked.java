/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantLong;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;

import java.util.Set;

public class ReadReturnShouldBeChecked extends BytecodeScanningDetector implements StatelessDetector {

    boolean sawRead = false;

    boolean sawSkip = false;

    boolean sawConversion = false;

    boolean recentCallToAvailable = false;

    int sawAvailable = 0;

    boolean wasBufferedInputStream = false;

    BugAccumulator accumulator;

    private int locationOfCall;

    private String lastCallClass = null, lastCallMethod = null, lastCallSig = null;

    private static final Set<Short> comparisons = Set.of(
            Const.IF_ICMPEQ, Const.IF_ICMPNE,
            Const.IF_ICMPLT, Const.IF_ICMPGE,
            Const.IF_ICMPGT, Const.IF_ICMPLE,
            Const.FCMPL, Const.FCMPG,
            Const.DCMPL, Const.DCMPG,
            Const.LCMP);

    public ReadReturnShouldBeChecked(BugReporter bugReporter) {
        this.accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        sawAvailable = 0;
        sawRead = false;
        sawSkip = false;
        sawConversion = false;
        super.visit(obj);
        accumulator.reportAccumulatedBugs();
    }

    private boolean isInputStream() {

        if (lastCallClass.startsWith("[")) {
            return false;
        }
        return (Subtypes2.instanceOf(lastCallClass, "java.io.InputStream")
                || Subtypes2.instanceOf(lastCallClass, "java.io.DataInput") || Subtypes2.instanceOf(lastCallClass,
                        "java.io.Reader")) && !Subtypes2.instanceOf(lastCallClass, "java.io.ByteArrayInputStream");

    }

    private boolean isBufferedInputStream() {
        try {
            if (lastCallClass.startsWith("[")) {
                return false;
            }
            return Repository.instanceOf(lastCallClass, "java.io.BufferedInputStream");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isImageIOInputStream() {
        try {
            if (lastCallClass.startsWith("[")) {
                return false;
            }
            return Repository.instanceOf(lastCallClass, "javax.imageio.stream.ImageInputStream");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            lastCallClass = getDottedClassConstantOperand();
            lastCallMethod = getNameConstantOperand();
            lastCallSig = getSigConstantOperand();
        }

        if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            if ("available".equals(getNameConstantOperand()) && "()I".equals(getSigConstantOperand())
                    || getNameConstantOperand().startsWith("get") && getNameConstantOperand().endsWith("Length")
                            && "()I".equals(getSigConstantOperand()) || "java/io/File".equals(getClassConstantOperand())
                                    && "length".equals(getNameConstantOperand()) && "()J".equals(getSigConstantOperand())) {
                sawAvailable = 70;
                return;
            }
        }
        sawAvailable--;
        if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE)
                && "read".equals(getNameConstantOperand())

                && ("([B)I".equals(getSigConstantOperand()) || "([BII)I".equals(getSigConstantOperand())
                        || "([C)I".equals(getSigConstantOperand()) || "([CII)I".equals(getSigConstantOperand()))
                && isInputStream()) {
            sawRead = true;
            recentCallToAvailable = sawAvailable > 0;
            locationOfCall = getPC();
            return;
        }
        if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE)
                && ("skip".equals(getNameConstantOperand()) && "(J)J".equals(getSigConstantOperand()) || "skipBytes".equals(getNameConstantOperand())
                        && "(I)I".equals(getSigConstantOperand())) && isInputStream()
                && !isImageIOInputStream()) {
            // if not ByteArrayInput Stream
            // and either no recent calls to length
            // or it is a BufferedInputStream

            wasBufferedInputStream = isBufferedInputStream();
            sawSkip = true;
            locationOfCall = getPC();
            recentCallToAvailable = sawAvailable > 0 && !wasBufferedInputStream;
            return;

        }

        if (sawRead && seen == Const.ICONST_M1 && comparisons.contains((short) getNextOpcode())) {
            accumulator.accumulateBug(
                    new BugInstance(this, "NCR_NOT_PROPERLY_CHECKED_READ", recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false),
                    SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));
        }

        if (sawConversion) {
            reportIfMinusOneConstant(getConstantRefOperand());
            sawConversion = false;
        }

        if (sawRead && (seen == Const.I2F || seen == Const.I2D || seen == Const.I2L) &&
                comparisons.contains((short) getNextCodeByte(2))) {
            sawConversion = true;
        }

        if ((seen == Const.POP) || (seen == Const.POP2)) {

            if (sawRead) {
                accumulator.accumulateBug(
                        new BugInstance(this, "RR_NOT_CHECKED", recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false),
                        SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));

            } else if (sawSkip) {

                accumulator.accumulateBug(
                        new BugInstance(this, "SR_NOT_CHECKED", (wasBufferedInputStream ? HIGH_PRIORITY
                                : recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)).addClassAndMethod(this)
                                .addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false), SourceLineAnnotation
                                .fromVisitedInstruction(getClassContext(), this, locationOfCall));
            }
        }

        sawRead = false;
        sawSkip = false;
    }

    private void reportIfMinusOneConstant(Constant c) {
        boolean isMinusOne = false;

        if (c instanceof ConstantFloat) {
            isMinusOne = ((ConstantFloat) c).getBytes() == -1.0f;
        } else if (c instanceof ConstantDouble) {
            isMinusOne = ((ConstantDouble) c).getBytes() == -1.0d;
        } else if (c instanceof ConstantLong) {
            isMinusOne = ((ConstantLong) c).getBytes() == -1L;
        }

        if (isMinusOne) {
            accumulator.accumulateBug(
                    new BugInstance(this, "NCR_NOT_PROPERLY_CHECKED_READ", recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false),
                    SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));
        }
    }
}
