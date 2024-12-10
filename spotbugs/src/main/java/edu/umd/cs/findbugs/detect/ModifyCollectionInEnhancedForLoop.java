package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import java.util.HashMap;
import java.util.Map;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST
    }

    private LoopState state = LoopState.INITIAL;
    Map<Integer, Integer> loopVariables = new HashMap<>();
    private int currentLoopStart = -1;

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
            if (!loopVariables.isEmpty() && loopVariables.containsKey(getRegisterOperand())) {
                BugInstance bug = new BugInstance(this, "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP", LOW_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this);
                bugAccumulator.accumulateBug(bug, this);
                state = LoopState.INITIAL;
            }
        }
        if (seen == Const.GOTO && !loopVariables.isEmpty()) {
            int gotoTarget = getBranchTarget();
            loopVariables.values().remove(gotoTarget);
            state = LoopState.INITIAL;
        }

        switch (state) {
        case INITIAL:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo("()Ljava/util/Iterator;")) {
                state = LoopState.ITERATOR_CREATE;
            }
            break;

        case ITERATOR_CREATE:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo("()Z")) {
                state = LoopState.HAS_NEXT;
            } else if (!isStore(seen) && !isLoad(seen)) {
                state = LoopState.INITIAL;
            }
            currentLoopStart = isLoad(seen) ? getPC() : currentLoopStart;
            break;

        case HAS_NEXT:
            if (seen == Const.INVOKEINTERFACE && isMethodSignatureEqualTo("()Ljava/lang/Object;")) {
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
                loopVariables.put(getRegisterOperand(), currentLoopStart);
            }
            state = LoopState.INITIAL;
            break;

        default:
            state = LoopState.INITIAL;
            break;
        }
    }

    private boolean isStore(int opcode) {
        return opcode >= 54 && opcode <= 86;
    }

    private boolean isLoad(int opcode) {
        return opcode >= 21 && opcode <= 53;
    }

    private boolean isMethodSignatureEqualTo(String signature) {
        XMethod xMethod = getXMethodOperand();
        return xMethod != null && signature.equals(xMethod.getSignature());
    }
}
