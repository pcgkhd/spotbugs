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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST
    }

    private LoopState state = LoopState.INITIAL;
    private final Map<Integer, Integer> variableToLoopStart = new HashMap<>();
    private int currentLoopStart;
    private static final Set<Short> STORE_OPCODES = Set.of(
            Const.ISTORE, Const.LSTORE, Const.FSTORE, Const.DSTORE, Const.ASTORE,
            Const.ISTORE_0, Const.ISTORE_1, Const.ISTORE_2, Const.ISTORE_3,
            Const.LSTORE_0, Const.LSTORE_1, Const.LSTORE_2, Const.LSTORE_3,
            Const.FSTORE_0, Const.FSTORE_1, Const.FSTORE_2, Const.FSTORE_3,
            Const.DSTORE_0, Const.DSTORE_1, Const.DSTORE_2, Const.DSTORE_3,
            Const.ASTORE_0, Const.ASTORE_1, Const.ASTORE_2, Const.ASTORE_3,
            Const.IASTORE, Const.LASTORE, Const.FASTORE, Const.DASTORE,
            Const.AASTORE, Const.BASTORE, Const.CASTORE, Const.SASTORE
    );

    private static final Set<Short> LOAD_OPCODES = Set.of(
            Const.ILOAD, Const.LLOAD, Const.FLOAD, Const.DLOAD, Const.ALOAD,
            Const.ILOAD_0, Const.ILOAD_1, Const.ILOAD_2, Const.ILOAD_3,
            Const.LLOAD_0, Const.LLOAD_1, Const.LLOAD_2, Const.LLOAD_3,
            Const.FLOAD_0, Const.FLOAD_1, Const.FLOAD_2, Const.FLOAD_3,
            Const.DLOAD_0, Const.DLOAD_1, Const.DLOAD_2, Const.DLOAD_3,
            Const.ALOAD_0, Const.ALOAD_1, Const.ALOAD_2, Const.ALOAD_3,
            Const.IALOAD, Const.LALOAD, Const.FALOAD, Const.DALOAD,
            Const.AALOAD, Const.BALOAD, Const.CALOAD, Const.SALOAD
    );

    public ModifyCollectionInEnhancedForLoop(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        state = LoopState.INITIAL;
    }

    @Override
    public void sawOpcode(int seen) {
        if (isStore(seen) && !variableToLoopStart.isEmpty() && variableToLoopStart.containsKey(getRegisterOperand())) {
            BugInstance bug = new BugInstance(this, "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP", LOW_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this);
            bugReporter.reportBug(bug);
            state = LoopState.INITIAL;
        }
        if (seen == Const.GOTO && !variableToLoopStart.isEmpty()) {
            int gotoTarget = getBranchTarget();
            variableToLoopStart.values().remove(gotoTarget);
            state = LoopState.INITIAL;
        }

        switch (state) {
        case INITIAL:
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Ljava/util/Iterator;", "iterator", getXMethodOperand(), true)) {
                state = LoopState.ITERATOR_CREATE;
            }
            break;

        case ITERATOR_CREATE:
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Z", "hasNext", getXMethodOperand(), false)) {
                state = LoopState.HAS_NEXT;
            } else if (!isStore(seen) && !isLoad(seen)) {
                state = LoopState.INITIAL;
            }
            currentLoopStart = isLoad(seen) ? getPC() : currentLoopStart;
            break;

        case HAS_NEXT:
            if (seen == Const.INVOKEINTERFACE && isMethodMatching("()Ljava/lang/Object;", "next", getXMethodOperand(), false)) {
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
}
