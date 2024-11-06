package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

public class ModifyCollectionInEnhancedForLoop extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    public ModifyCollectionInEnhancedForLoop(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/util/Iterator;")) {
            // detect iterator creation
        } else if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Z")) {
            // detect hasNext call
        } else if (seen == Const.INVOKEINTERFACE && getXMethodOperand().getSignature().equals("()Ljava/lang/Object;")) {
            // detect next call
        } else if (seen == Const.CHECKCAST) {
            // detect casting back to the correct type
        } else if (seen == Const.ASTORE) {
            // detect assignment to the loop variable
        }
        // should save these with sawopcode
    }
}
