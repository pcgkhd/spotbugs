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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import java.util.HashMap;
import java.util.Map;

public class FindEnhancedForLoopVariableModification extends OpcodeStackDetector {

    private enum CollectionLoopState {
        INITIAL, ITERATOR_CREATE, ITERATOR_STORE, COLLECTION_CONDITION, HAS_NEXT
    }

    private enum ArrayLoopState {
        INITIAL, ARRAY_STORE, ARRAY_SIZE_STORE, LOOP_VARIABLE_STORE, ARRAY_CONDITION
    }

    private final BugReporter bugReporter;
    private final Map<LocalVariable, Integer> loopVariableToConditionPosition = new HashMap<>();

    private CollectionLoopState collectionLoopState = CollectionLoopState.INITIAL;
    private ArrayLoopState arrayLoopState = ArrayLoopState.INITIAL;

    private int arrayIndexRegisterOperand;
    private int iteratorRegisterOperand;
    private int collectionLoopStart;
    private int arrayLoopConditionStart;

    public FindEnhancedForLoopVariableModification(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        collectionLoopState = CollectionLoopState.INITIAL;
        arrayLoopState = ArrayLoopState.INITIAL;
        collectionLoopStart = -1;
        arrayLoopConditionStart = -1;
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
        LocalVariable variable = isRegisterStore() ? getLocalVariable() : null;

        if (variable != null && loopVariableToConditionPosition.containsKey(variable)) {
            BugInstance bug = new BugInstance(this, "MEV_MODIFY_ENHANCED_FOR_LOOP_VARIABLE", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this)
                    .add(new LocalVariableAnnotation(variable.getName(), variable.getIndex(), this.getPC()));

            bugReporter.reportBug(bug);
            collectionLoopState = CollectionLoopState.INITIAL;
            arrayLoopState = ArrayLoopState.INITIAL;
        }

        if (seen == Const.GOTO && !loopVariableToConditionPosition.isEmpty()) {
            int gotoTarget = getBranchTarget();
            loopVariableToConditionPosition.values().remove(gotoTarget);
            collectionLoopState = CollectionLoopState.INITIAL;
            arrayLoopState = ArrayLoopState.INITIAL;
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
            if (seen == Const.INVOKEINTERFACE && getXMethodOperand() != null &&
                    "()Ljava/util/Iterator;".equals(getXMethodOperand().getSignature()) && "iterator".equals(getXMethodOperand().getName())) {
                collectionLoopState = CollectionLoopState.ITERATOR_CREATE;
            }
            break;

        case ITERATOR_CREATE:
            // Synthetic variable which has no name in LVT. Storing the iterator of the Collection
            if (isRegisterStore() && getLocalVariable() == null) {
                collectionLoopState = CollectionLoopState.ITERATOR_STORE;
                iteratorRegisterOperand = getRegisterOperand();
            } else {
                collectionLoopState = CollectionLoopState.INITIAL;
            }
            break;

        case ITERATOR_STORE:
            // Loading iterator to call the hasNext method is the start of the condition. Every iteration jumps back here (GOTO)
            if (isRegisterLoad() && iteratorRegisterOperand == getRegisterOperand()) {
                collectionLoopStart = getPC();
                collectionLoopState = CollectionLoopState.COLLECTION_CONDITION;
            } else {
                collectionLoopState = CollectionLoopState.INITIAL;
            }
            break;

        case COLLECTION_CONDITION:
            // The condition of the loop should be the iterators hasNext() method
            if (seen == Const.INVOKEINTERFACE && getXMethodOperand() != null && "()Z".equals(getXMethodOperand().getSignature())
                    && "hasNext".equals(getXMethodOperand().getName()) && "java.util.Iterator".equals(getXMethodOperand().getClassName())) {
                collectionLoopState = CollectionLoopState.HAS_NEXT;
            } else {
                collectionLoopState = CollectionLoopState.INITIAL;
            }
            break;

        case HAS_NEXT:
            // Check if the next element of the iterator is used
            if (seen == Const.INVOKEINTERFACE && getXMethodOperand() != null && "()Ljava/lang/Object;".equals(getXMethodOperand().getSignature())
                    && "next".equals(getXMethodOperand().getName()) && "java.util.Iterator".equals(getXMethodOperand().getClassName())
                    && (getNextOpcode() == Const.POP || getNextOpcode() == Const.POP2)) {
                collectionLoopState = CollectionLoopState.INITIAL;
            }

            // Storing the next value of the iterator (the actual loop variable)
            if (isRegisterStore()) {
                LocalVariable localVariable = getLocalVariable();
                if (localVariable != null) {
                    loopVariableToConditionPosition.put(localVariable, collectionLoopStart);
                }
                collectionLoopState = CollectionLoopState.INITIAL;
                collectionLoopStart = -1;
            }
            break;

        default:
            collectionLoopState = CollectionLoopState.INITIAL;
            break;
        }
    }

    private void checkArrayEnhancedLoop(int seen) {
        switch (arrayLoopState) {
        case INITIAL:
            // Synthetic variable which has no name in LVT. Might be the start of enhanced loop storing the array.
            if ((seen == Const.ASTORE || seen == Const.ASTORE_0 || seen == Const.ASTORE_1 ||
                    seen == Const.ASTORE_2 || seen == Const.ASTORE_3) && getLocalVariable() == null) {
                arrayLoopState = ArrayLoopState.ARRAY_STORE;
            }
            break;

        case ARRAY_STORE:
            // Synthetic variable which has no name in LVT. Storing the size of the array, stored earlier
            if (isRegisterStore()) {
                if (getLocalVariable() == null && getPrevOpcode(1) == Const.ARRAYLENGTH) {
                    arrayLoopState = ArrayLoopState.ARRAY_SIZE_STORE;
                } else {
                    arrayLoopState = ArrayLoopState.INITIAL;
                }
            }
            break;

        case ARRAY_SIZE_STORE:
            // Synthetic variable which has no name in LVT. Storing the hidden iterator.
            if (isRegisterStore()) {
                if (getLocalVariable() == null && getPrevOpcode(1) == Const.ICONST_0) {
                    arrayIndexRegisterOperand = getRegisterOperand();
                    arrayLoopState = ArrayLoopState.LOOP_VARIABLE_STORE;
                } else {
                    arrayLoopState = ArrayLoopState.INITIAL;
                }
            }
            break;

        case LOOP_VARIABLE_STORE:
            // The start of the condition, compares the iterator and the array size. The end of the loop (GOTO) jumps back here.
            if (isRegisterLoad() && arrayIndexRegisterOperand == getRegisterOperand()) {
                arrayLoopConditionStart = getPC();
            }
            if (seen == Const.IF_ICMPGE) {
                arrayLoopState = ArrayLoopState.ARRAY_CONDITION;
            }
            break;

        case ARRAY_CONDITION:
            // After the condition it stores the actual loop variable
            LocalVariable variable = isRegisterStore() ? getLocalVariable() : null;
            if (variable != null) {
                loopVariableToConditionPosition.put(variable, arrayLoopConditionStart);
                arrayLoopState = ArrayLoopState.INITIAL;
                arrayLoopConditionStart = -1;
            }
            break;

        default:
            arrayLoopState = ArrayLoopState.INITIAL;
            break;
        }
    }

    /**
     * Returns the local variable at the current store opcode.
     * Should only be called at a register store opcode.
     */
    private LocalVariable getLocalVariable() {
        LocalVariableTable lvt = getMethod().getLocalVariableTable();
        return (lvt != null) ? lvt.getLocalVariable(getRegisterOperand(), getNextPC()) : null;
    }
}
