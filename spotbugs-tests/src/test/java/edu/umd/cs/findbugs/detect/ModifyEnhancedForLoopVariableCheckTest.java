package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ModifyEnhancedForLoopVariableCheckTest extends AbstractIntegrationTest {

    private static final String MEV_BUG_TYPE = "MEV_MODIFY_ENHANCED_FOR_LOOP_VARIABLE";

    @Test
    void testBadEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/BadEnhanceForLoopUsageCheckTest.class");

        assertBugTypeCount(MEV_BUG_TYPE, 14);

        final String className = "BadEnhanceForLoopUsageCheckTest";
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", "i", 16);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyStringElement", "name", 31);
        assertBugAtVar(MEV_BUG_TYPE, className, "loopWithinLoop", "number", 40);
        assertBugAtVar(MEV_BUG_TYPE, className, "notEnhancedLoopWithinEnhancedLoops", "number", 51);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyOuterLoopVariable", "numbers", 66);
        assertBugAtVar(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", "number", 75);
        assertBugAtVar(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", "numbers", 77);
        assertBugAtVar(MEV_BUG_TYPE, className, "arrayEnhancedForLoop", "number", 85);
        assertBugAtVar(MEV_BUG_TYPE, className, "charArrayEnhancedForLoop", "character", 93);
        assertBugAtVar(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", "character", 105);
        assertBugAtVar(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", "number", 107);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyOuterEnhancedLoopVariable", "outer", 118);
        assertBugAtVar(MEV_BUG_TYPE, className, "arrayEnhancedLoopWithConversionsAndMethodCalls", "d", 126);
        assertBugAtVar(MEV_BUG_TYPE, className, "falsePositive", "num", 135); // This should not report bug, as it modifies the variable on purpose
    }

    @Test
    void testGoodEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/GoodEnhanceForLoopUsageCheckTest.class");

        assertNoBugType(MEV_BUG_TYPE);
    }
}
