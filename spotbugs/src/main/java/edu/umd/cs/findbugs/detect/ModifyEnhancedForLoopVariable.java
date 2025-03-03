/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import java.util.HashMap;
import java.util.Map;

public class ModifyEnhancedForLoopVariable extends OpcodeStackDetector {
    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST, CONDITION, ARRAY_STORE, ARRAY_SIZE_STORE, LOOP_VARIABLE_STORE
    }

    private int arrayLoopVariable;

    private final BugReporter bugReporter;

    private LoopState collectionLoopState = LoopState.INITIAL;
    private LoopState arrayLoopState = LoopState.INITIAL;
    private final Map<LocalVariable, Integer> loopVariableToConditionPosition = new HashMap<>();
    private int collectionLoopStart;
    private int arrayLoopConditionStart;
    private int collectionOpcodeCounter;

    public ModifyEnhancedForLoopVariable(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        collectionLoopState = LoopState.INITIAL;
        arrayLoopState = LoopState.INITIAL;
    }

    /**
     * <a href="https://docs.oracle.com/javase/1.5.0/docs/guide/language/foreach.html">JSL foreach</a>
     *
     * There are two ways to create enhanced loop in Java.
     * One is by using Collection as the data source. In this occasion java creates an iterator, and iterates through the
     * Collection with this iterator.
     * The other way is to use array as the data source. This time Java creates index variable instead of iterator.
     */
    @Override
    public void sawOpcode(int seen) {
        LocalVariable variable = getLocalVariable();

        if (!loopVariableToConditionPosition.isEmpty() && variable != null && loopVariableToConditionPosition.containsKey(variable)) {
            BugInstance bug = new BugInstance(this, "MEV_MODIFY_ENHANCED_FOR_LOOP_VARIABLE", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this)
                    .add(new LocalVariableAnnotation(variable.getName(), variable.getIndex(), this.getPC()));

            bugReporter.reportBug(bug);
            collectionLoopState = LoopState.INITIAL;
            arrayLoopState = LoopState.INITIAL;
        }

        if (seen == Const.GOTO && !loopVariableToConditionPosition.isEmpty()) {
            int gotoTarget = getBranchTarget();
            loopVariableToConditionPosition.values().remove(gotoTarget);
            resetCollectionState();
            resetArrayState();
        }

        checkCollectionEnhancedLoop(seen);
        checkArrayEnhancedLoop(seen);
    }

    private void checkCollectionEnhancedLoop(int seen) {
        switch (collectionLoopState) {
        case INITIAL:
            // Initial state: waiting for the `iterator()` method call. The class that contains the 'iterator()' method is not checked,
            // because it can be any of Collection subclasses
            // The `iterator()` method call indicates the start of an enhanced `for` loop.
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Ljava/util/Iterator;", "iterator", getXMethodOperand(), true)) {
                collectionOpcodeCounter++;
                collectionLoopState = LoopState.ITERATOR_CREATE;
            }
            break;

        case ITERATOR_CREATE:
            // Represents the number of bytecodes preceding the loading of the loop variable (for condition check),
            // ensuring a continuous bytecode sequence. It marks the entry point of each iteration (where the goto jumps).
            int forLoopVariableByteCodeNumbers = 3;

            // After the `iterator()` method call, wait for the `hasNext()` method call.
            // The `hasNext()` method call indicates the condition check of the loop.
            collectionOpcodeCounter++;
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Z", "hasNext", getXMethodOperand(), false)) {
                collectionLoopState = LoopState.HAS_NEXT;
            } else if (isRegisterLoad() && collectionOpcodeCounter == forLoopVariableByteCodeNumbers) {
                collectionLoopStart = isRegisterLoad() ? getPC() : collectionLoopStart;
            } else if (!isRegisterStore() && !isRegisterLoad()) {
                resetCollectionState();
            }
            break;

        case HAS_NEXT:
            // IFEQ opcode checks if the 'hasNext()' method returns with true value
            // After the `hasNext()` method call, wait for the `next()` method call.
            // The `next()` method call indicates the start of the loop body.
            collectionOpcodeCounter++;
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Ljava/lang/Object;", "next", getXMethodOperand(), false)) {
                collectionLoopState = LoopState.NEXT_CALL;
            } else if (seen != Const.IFEQ && !isRegisterLoad()) {
                resetCollectionState();
            }
            break;

        case NEXT_CALL:
            // After the `next()` method call, wait for the `CHECKCAST` opcode.
            // The `CHECKCAST` opcode indicates that we are checking the type of the loop variable.
            collectionOpcodeCounter++;
            if (seen == Const.CHECKCAST) {
                collectionLoopState = LoopState.TYPE_CAST;
            } else {
                resetCollectionState();
            }
            break;

        case TYPE_CAST:
            // Represents the number of bytecodes that precede the storage of the enhanced loop variable
            // Ensures that the bytecode sequence before storing the loop variable in an enhanced loop is continuous.
            int enhancedLoopByteCodeNumbers = 8;

            // After the `CHECKCAST` opcode, save the loop variable.
            // Later if this loop variable is modified, then bug should be reported.
            LocalVariable localVariable = getLocalVariable();
            if (isRegisterStore() && collectionOpcodeCounter == enhancedLoopByteCodeNumbers && localVariable != null) {
                loopVariableToConditionPosition.put(localVariable, collectionLoopStart);
            }
            resetCollectionState();
            break;

        default:
            resetCollectionState();
            break;
        }
    }

    private void checkArrayEnhancedLoop(int seen) {
        switch (arrayLoopState) {
        case INITIAL:
            // Synthetic variable which has no name in LVT. Might be the start of enhanced loop storing the array.
            if (isRegisterStore() && getLocalVariable() == null) {
                arrayLoopState = LoopState.ARRAY_STORE;
            }
            break;

        case ARRAY_STORE:
            // Synthetic variable which has no name in LVT. Storing the size of the array, stored earlier
            if (isRegisterStore()) {
                if (getLocalVariable() == null && getPrevOpcode(1) == Const.ARRAYLENGTH) {
                    arrayLoopState = LoopState.ARRAY_SIZE_STORE;
                } else {
                    resetArrayState();
                }
            }
            break;

        case ARRAY_SIZE_STORE:
            // Synthetic variable which has no name in LVT. Storing the hidden iterator.
            if (isRegisterStore()) {
                if (getLocalVariable() == null && getPrevOpcode(1) == Const.ICONST_0) {
                    arrayLoopVariable = getRegisterOperand();
                    arrayLoopState = LoopState.LOOP_VARIABLE_STORE;
                } else {
                    resetArrayState();
                }
            }
            break;

        case LOOP_VARIABLE_STORE:
            // The start of the condition, compares the iterator and the array size. The end of the loop (GOTO) jumps back here.
            if (isRegisterLoad() && arrayLoopVariable == getRegisterOperand()) {
                arrayLoopConditionStart = getPC();
            }
            if (seen == Const.IF_ICMPGE) {
                arrayLoopState = LoopState.CONDITION;
            }

        case CONDITION:
            // After the condition it stores the actual loop variable
            LocalVariable variable = getLocalVariable();
            if (isRegisterStore() && variable != null) {
                loopVariableToConditionPosition.put(variable, arrayLoopConditionStart);
                resetArrayState();
            }
            break;

        default:
            resetArrayState();
            break;
        }
    }

    private static boolean isMethodMatching(String expectedSignature, String expectedMethodName, XMethod methodToCheck,
            boolean skipIteratorCheck) {
        return methodToCheck != null
                && expectedSignature.equals(methodToCheck.getSignature())
                && expectedMethodName.equals(methodToCheck.getName())
                && (skipIteratorCheck || "java.util.Iterator".equals(methodToCheck.getClassName()));
    }

    private void resetCollectionState() {
        collectionOpcodeCounter = 0;
        collectionLoopState = LoopState.INITIAL;
    }

    private void resetArrayState() {
        arrayLoopState = LoopState.INITIAL;
    }

    private LocalVariable getLocalVariable() {
        LocalVariableTable lvt = getMethod().getLocalVariableTable();
        return (lvt != null && isRegisterStore()) ? lvt.getLocalVariable(getRegisterOperand(), getNextPC()) : null;
    }
}
