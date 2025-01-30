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
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModifyEnhancedForLoopVariable extends OpcodeStackDetector {
    private static final int COLLECTION_ENHANCED_LOOP_BYTECODE_LENGTH = 8;
    private static final int COLLECTION_ENHANCED_LOOP_VARIABLE_POSITION = 3;
    private static final int ARRAY_ENHANCED_LOOP_BYTECODE_LENGTH = 14;
    private static final int ARRAY_ENHANCED_LOOP_CONDITION_POSITION = 10;
    private static final int ARRAY_ENHANCED_LOOP_VARIABLE_POSITION = 8;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST, VARIABLE_CREATE, CONDITION
    }

    private static final Set<Short> STORE_OPCODES = Set.of(
            Const.ISTORE, Const.LSTORE, Const.FSTORE, Const.DSTORE, Const.ASTORE,
            Const.ISTORE_0, Const.ISTORE_1, Const.ISTORE_2, Const.ISTORE_3,
            Const.LSTORE_0, Const.LSTORE_1, Const.LSTORE_2, Const.LSTORE_3,
            Const.FSTORE_0, Const.FSTORE_1, Const.FSTORE_2, Const.FSTORE_3,
            Const.DSTORE_0, Const.DSTORE_1, Const.DSTORE_2, Const.DSTORE_3,
            Const.ASTORE_0, Const.ASTORE_1, Const.ASTORE_2, Const.ASTORE_3,
            Const.IASTORE, Const.LASTORE, Const.FASTORE, Const.DASTORE,
            Const.AASTORE, Const.BASTORE, Const.CASTORE, Const.SASTORE);

    private static final Set<Short> LOAD_OPCODES = Set.of(
            Const.ILOAD, Const.LLOAD, Const.FLOAD, Const.DLOAD, Const.ALOAD,
            Const.ILOAD_0, Const.ILOAD_1, Const.ILOAD_2, Const.ILOAD_3,
            Const.LLOAD_0, Const.LLOAD_1, Const.LLOAD_2, Const.LLOAD_3,
            Const.FLOAD_0, Const.FLOAD_1, Const.FLOAD_2, Const.FLOAD_3,
            Const.DLOAD_0, Const.DLOAD_1, Const.DLOAD_2, Const.DLOAD_3,
            Const.ALOAD_0, Const.ALOAD_1, Const.ALOAD_2, Const.ALOAD_3,
            Const.IALOAD, Const.LALOAD, Const.FALOAD, Const.DALOAD,
            Const.AALOAD, Const.BALOAD, Const.CALOAD, Const.SALOAD);

    private final BugReporter bugReporter;

    private LoopState collectionLoopState = LoopState.INITIAL;
    private LoopState arrayLoopState = LoopState.INITIAL;
    private final Map<LocalVariable, Integer> loopVariableToInitPosition = new HashMap<>();
    private int collectionLoopStart;
    private int arrayLoopStart;
    private int arrayOpcodeCounter;
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
     * https://docs.oracle.com/javase/1.5.0/docs/guide/language/foreach.html
     *
     * There are two ways to create enhanced loop in Java.
     * One is by using Collection as the data source. In this occasion java creates an iterator, and iterates through the
     * Collection with this iterator.
     * The other way is to use array as the data source. This time Java creates index variable instead of iterator.
     */
    @Override
    public void sawOpcode(int seen) {
        if (isStore(seen) && !loopVariableToInitPosition.isEmpty() && loopVariableToInitPosition.containsKey(getLocalVariable())) {
            BugInstance bug = new BugInstance(this, "MEV_ENHANCED_FOR_LOOP_VARIABLE", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this);
            bugReporter.reportBug(bug);
            collectionLoopState = LoopState.INITIAL;
            arrayLoopState = LoopState.INITIAL;
        }

        if (seen == Const.GOTO && !loopVariableToInitPosition.isEmpty()) {
            int gotoTarget = getBranchTarget();
            loopVariableToInitPosition.values().remove(gotoTarget);
            collectionLoopState = LoopState.INITIAL;
            arrayLoopState = LoopState.INITIAL;
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
            // After the `iterator()` method call, wait for the `hasNext()` method call.
            // The `hasNext()` method call indicates the condition check of the loop.
            collectionOpcodeCounter++;
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Z", "hasNext", getXMethodOperand(), false)) {
                collectionLoopState = LoopState.HAS_NEXT;
            } else if (isLoad(seen) && collectionOpcodeCounter == COLLECTION_ENHANCED_LOOP_VARIABLE_POSITION) {
                collectionLoopStart = isLoad(seen) ? getPC() : collectionLoopStart;
            } else if (!isStore(seen) && !isLoad(seen)) {
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
            } else if (seen != Const.IFEQ && !isLoad(seen)) {
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
            // After the `CHECKCAST` opcode, save the loop variable.
            // Later if this loop variable is modified, then bug should be reported.
            if (isStore(seen) && collectionOpcodeCounter == COLLECTION_ENHANCED_LOOP_BYTECODE_LENGTH) {
                loopVariableToInitPosition.put(getLocalVariable(), collectionLoopStart);
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
            // Initial state: waiting for a `load` opcode to start tracking the array loop.
            // A `load` opcode indicates that the array reference is being loaded onto the stack.
            if (isLoad(seen)) {
                arrayOpcodeCounter++;
                arrayLoopState = LoopState.VARIABLE_CREATE;
            }
            break;

        case VARIABLE_CREATE:
            // After the array reference is loaded, track the initialization of the loop variables.
            // This includes loading the array length, initializing the loop variable, and checking the loop condition.
            arrayOpcodeCounter++;
            if (seen == Const.IF_ICMPGE && arrayOpcodeCounter == ARRAY_ENHANCED_LOOP_CONDITION_POSITION) {
                // The `IF_ICMPGE` opcode checks if the loop variable is greater than or equal to the array length.
                // This indicates the loop condition check.
                arrayLoopState = LoopState.CONDITION;
            } else if (isLoad(seen) && arrayOpcodeCounter == ARRAY_ENHANCED_LOOP_VARIABLE_POSITION) {
                // If we see a `load` opcode at the expected step, store the starting point of the loop.
                arrayLoopStart = isLoad(seen) ? getPC() : collectionLoopStart;
            } else if ((!isStore(seen) && !isLoad(seen) && seen != Const.ARRAYLENGTH && seen != Const.ICONST_0)
                    || arrayOpcodeCounter >= ARRAY_ENHANCED_LOOP_CONDITION_POSITION) {
                resetArrayState();
            }
            break;

        case CONDITION:
            // After the loop condition check, enhanced loop stores an array element into a variable, using the loop variable as the index
            // Later if this variable is modified, then bug should be reported.
            arrayOpcodeCounter++;
            if (isStore(seen) && arrayOpcodeCounter == ARRAY_ENHANCED_LOOP_BYTECODE_LENGTH) {
                loopVariableToInitPosition.put(getLocalVariable(), arrayLoopStart);
                resetArrayState();
            } else if (!isLoad(seen) || arrayOpcodeCounter >= ARRAY_ENHANCED_LOOP_BYTECODE_LENGTH) {
                resetArrayState();
            }
            break;

        default:
            resetArrayState();
            break;
        }
    }

    private boolean isStore(int opcode) {
        return STORE_OPCODES.contains((short) opcode);
    }

    private boolean isLoad(int opcode) {
        return LOAD_OPCODES.contains((short) opcode);
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
        arrayOpcodeCounter = 0;
        arrayLoopState = LoopState.INITIAL;
    }

    private LocalVariable getLocalVariable() {
        LocalVariableTable lvt = getMethod().getLocalVariableTable();
        return (lvt != null && isRegisterStore()) ? lvt.getLocalVariable(getRegisterOperand(), getNextPC()) : null;
    }
}
