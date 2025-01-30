package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class ModifyEnhancedForLoopVariableCheckTest extends AbstractIntegrationTest {

    private static final String MEV_BUG_TYPE = "MEV_ENHANCED_FOR_LOOP_VARIABLE";

    @Test
    void testBadEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/BadEnhanceForLoopUsageCheckTest.class");

        assertBugTypeCount(MEV_BUG_TYPE, 11);

        final String className = "BadEnhanceForLoopUsageCheckTest";
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", 16);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "modifyStringElement", 31);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "loopWithinLoop", 40);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "notEnhancedLoopWithinEnhancedLoops", 51);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "modifyOuterLoopVariable", 66);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", 75);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", 77);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "arrayEnhancedForLoop", 85);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "charArrayEnhancedForLoop", 93);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", 105);
        assertBugInMethodAtLine(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", 107);
    }

    @Test
    void testGoodEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/GoodEnhanceForLoopUsageCheckTest.class");

        assertNoBugType(MEV_BUG_TYPE);
    }
}
