package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    private enum LoopState {
        INITIAL, ITERATOR_CREATE, HAS_NEXT, NEXT_CALL, TYPE_CAST, ASSIGNMENT
    }

    private LoopState state = LoopState.INITIAL;
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
        switch (state) {
            case INITIAL:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/util/Iterator;")) {
                    System.out.println("Iterator created");
                    state = LoopState.ITERATOR_CREATE;
                }
                break;

            case ITERATOR_CREATE:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Z")) {
                    System.out.println("hasNext called");
                    state = LoopState.HAS_NEXT;
                } else if (seen != Const.ASTORE_3 && seen != Const.ALOAD_3) {
                    state = LoopState.INITIAL;
                }
                break;

            case HAS_NEXT:
                if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/lang/Object;")) {
                    System.out.println("next called");
                    state = LoopState.NEXT_CALL;
                } else if (seen != Const.IFEQ && seen != Const.ALOAD_3) {
                    state = LoopState.INITIAL;
                }
                break;

            case NEXT_CALL:
                if (seen == Const.CHECKCAST) {
                    System.out.println("Typecast detected");
                    state = LoopState.TYPE_CAST;
                } else {
                    state = LoopState.INITIAL;
                }
                break;

            case TYPE_CAST:
                if (seen == Const.ASTORE) {
                    loopVariableIndex = getRegisterOperand();
                    System.out.println("Variable stored");
                    state = LoopState.ASSIGNMENT;
                    System.out.println("Enhanced for loop detected");
                } else {
                    state = LoopState.INITIAL;
                }
                break;

            case ASSIGNMENT:
                if (seen == Const.ASTORE) {
                    int currentVariableIndex = getRegisterOperand();
                    if (currentVariableIndex == loopVariableIndex) {
                        BugInstance bug = new BugInstance(this, "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP", LOW_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this);
                        bugAccumulator.accumulateBug(bug, this);
                    }
                }
                break;

            default:
                state = LoopState.INITIAL;
                break;
        }
    }
}
