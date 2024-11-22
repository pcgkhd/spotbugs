package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import java.util.ArrayDeque;
import java.util.Deque;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST
    }

    private LoopState state = LoopState.INITIAL;
    Deque<Integer> loopVariables = new ArrayDeque<>();
    private int loopVariableIndex = -1;

    public ModifyCollectionInEnhancedForLoop(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
        state = LoopState.INITIAL;
        loopVariableIndex = -1;
    }

    @Override
    public void sawOpcode(int seen) {
        if (isAstore(seen)) {
            int currentVariableIndex = getRegisterOperand();
            if (loopVariables.contains(currentVariableIndex)) {
                BugInstance bug = new BugInstance(this, "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP", LOW_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this);
                bugAccumulator.accumulateBug(bug, this);
                state = LoopState.INITIAL;
            }
        }
        if (seen == Const.GOTO && !loopVariables.isEmpty()) {
            loopVariables.removeLast();
            state = LoopState.INITIAL;
        }

        switch (state) {
            case INITIAL:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/util/Iterator;")) {
                    state = LoopState.ITERATOR_CREATE;
                }
                break;

            case ITERATOR_CREATE:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Z")) {
                    state = LoopState.HAS_NEXT;
                } else if (!isAstore(seen) && !isAload(seen)) {
                    state = LoopState.INITIAL;
                }
                break;

            case HAS_NEXT:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/lang/Object;")) {
                    state = LoopState.NEXT_CALL;
                } else if (seen != Const.IFEQ && !isAload(seen)) {
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
                if (isAstore(seen)) {
                    loopVariableIndex = getRegisterOperand();
                    loopVariables.add(getRegisterOperand());
                }
                state = LoopState.INITIAL;
                break;

            default:
                state = LoopState.INITIAL;
                break;
        }
    }

    private boolean isAstore(int opcode) {
        return opcode == Const.ASTORE || opcode == Const.ASTORE_0 || opcode == Const.ASTORE_1
                || opcode == Const.ASTORE_2 || opcode == Const.ASTORE_3;
    }

    private boolean isAload(int opcode) {
        return opcode == Const.ALOAD || opcode == Const.ALOAD_0 || opcode == Const.ALOAD_1
                || opcode == Const.ALOAD_2 || opcode == Const.ALOAD_3;
    }
}
