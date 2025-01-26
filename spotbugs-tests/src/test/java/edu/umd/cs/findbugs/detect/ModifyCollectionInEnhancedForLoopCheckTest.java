package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class ModifyCollectionInEnhancedForLoopCheckTest extends AbstractIntegrationTest {

    private static final String MCE_BUG_TYPE = "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP";

    @Test
    void testBadEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/BadEnhanceForLoopUsageCheckTest.class");

        assertBugTypeCount(MCE_BUG_TYPE, 11);

        final String className = "BadEnhanceForLoopUsageCheckTest";
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", 16);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyStringElement", 31);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "loopWithinLoop", 40);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "notEnhancedLoopWithinEnhancedLoops", 51);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyOuterLoopVariable", 66);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", 75);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", 77);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "arrayEnhancedForLoop", 85);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "charArrayEnhancedForLoop", 93);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", 105);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", 107);
    }

    @Test
    void testGoodEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/GoodEnhanceForLoopUsageCheckTest.class");

        assertNoBugType(MCE_BUG_TYPE);
    }
}
