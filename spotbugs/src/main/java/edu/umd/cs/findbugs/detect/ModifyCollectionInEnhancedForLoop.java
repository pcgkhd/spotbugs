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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST
    }

    private LoopState state = LoopState.INITIAL;
    private final Map<Integer, Integer> variableToLoopStart = new HashMap<>();
    private int currentLoopStart;
    private static final Set<Integer> STORE_OPCODES = new HashSet<>();
    private static final Set<Integer> LOAD_OPCODES = new HashSet<>();

    static {
        for (int i = Const.ISTORE; i <= Const.SASTORE; i++) {
            STORE_OPCODES.add(i);
        }
        for (int i = Const.ILOAD; i <= Const.SALOAD; i++) {
            LOAD_OPCODES.add(i);
        }
    }

    public ModifyCollectionInEnhancedForLoop(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
        state = LoopState.INITIAL;
    }

    @Override
    public void sawOpcode(int seen) {
        if (isStore(seen) && isRegisterStore()) {
            if (!variableToLoopStart.isEmpty() && variableToLoopStart.containsKey(getRegisterOperand())) {
                BugInstance bug = new BugInstance(this, "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP", LOW_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this);
                bugAccumulator.accumulateBug(bug, this);
                state = LoopState.INITIAL;
            }
        }
        if (seen == Const.GOTO && !variableToLoopStart.isEmpty()) {
            int gotoTarget = getBranchTarget();
            variableToLoopStart.values().remove(gotoTarget);
            state = LoopState.INITIAL;
        }

        switch (state) {
        case INITIAL:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo(getXMethodOperand())) {
                state = LoopState.ITERATOR_CREATE;
            }
            break;

        case ITERATOR_CREATE:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo("()Z", "hasNext", getXMethodOperand())) {
                state = LoopState.HAS_NEXT;
            } else if (!isStore(seen) && !isLoad(seen)) {
                state = LoopState.INITIAL;
            }
            currentLoopStart = isLoad(seen) ? getPC() : currentLoopStart;
            break;

        case HAS_NEXT:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo("()Ljava/lang/Object;", "next", getXMethodOperand())) {
                state = LoopState.NEXT_CALL;
            } else if (seen != Const.IFEQ && !isLoad(seen)) {
                state = LoopState.INITIAL;
            }
            break;

        case NEXT_CALL:
            if (seen == Const.CHECKCAST) {
                state = LoopState.TYPE_CAST;
            } else {
                state = LoopState.INITIAL;
            }
            break;

        case TYPE_CAST:
            if (isStore(seen)) {
                variableToLoopStart.put(getRegisterOperand(), currentLoopStart);
            }
            state = LoopState.INITIAL;
            break;

        default:
            state = LoopState.INITIAL;
            break;
        }
    }

    private boolean isStore(int opcode) {
        return STORE_OPCODES.contains(opcode);
    }

    private boolean isLoad(int opcode) {
        return LOAD_OPCODES.contains(opcode);
    }

    private static boolean isMethodSignatureEqualTo(String signature, String methodName, XMethod xMethod) {
        return xMethod != null && signature.equals(xMethod.getSignature()) && methodName.equals(xMethod.getName()) && "java/util/Iterator".equals(xMethod.getMethodDescriptor().getSlashedClassName());
    }

    private static boolean isMethodSignatureEqualTo(XMethod xMethod) {
        return xMethod != null && "()Ljava/util/Iterator;".equals(xMethod.getSignature()) && "iterator".equals(xMethod.getName());
    }
}
