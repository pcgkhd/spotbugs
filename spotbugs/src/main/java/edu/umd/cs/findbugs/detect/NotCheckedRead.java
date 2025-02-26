package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

public class NotCheckedRead extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public NotCheckedRead(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        // Check for INVOKEVIRTUAL or INVOKEINTERFACE (method calls)
        if (seen == Const.INVOKEVIRTUAL) {
            // Get the method name and signature
            String methodName = getNameConstantOperand();
            String methodSig = getSigConstantOperand();

            // Check if the method is 'read' and it takes an array as argument (byte[] or char[])
            if (methodName.equals("read") && (methodSig.equals("([B)I") || methodSig.equals("([C)I"))) {
                // At this point, we've identified a read() call that fills an array

                // Check if the return value of the read method is used or ignored
                if (stack.getStackDepth() > 0 && stack.getStackItem(0).getSignature().equals("I")) {
                    // The return value (int) is on the stack, now check if it's used
                    if (isIgnoredReturn()) {
                        // If the return value is ignored, report a bug
                        bugReporter.reportBug(new BugInstance(this, "NCR_NOT_CHECKED_READ", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this));
                    }
                }
            }
        }
    }

    /**
     * Checks if the return value of the method is ignored.
     * This can be done by checking the following opcode after the method call.
     */
    private boolean isIgnoredReturn() {
        // Check the next instruction after the read() method call
        int nextOpcode = getNextOpcode();

        // If the next opcode is POP or POP2, it indicates the return value is ignored
        return nextOpcode == Const.POP || nextOpcode == Const.POP2;
    }
}
