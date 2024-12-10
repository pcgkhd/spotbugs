package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class ModifyCollectionInEnhancedForLoopCheckTest extends AbstractIntegrationTest {

    private static final String MCE_BUG_TYPE = "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP";

    @Test
    void testBadEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/BadEnhanceForLoopUsageCheckTest.class");

        assertBugTypeCount(MCE_BUG_TYPE, 6);

        final String className = "BadEnhanceForLoopUsageCheckTest";
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", 16);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyElementDirectly", 32);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyArrayListDirectly", 40);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "loopWithinLoop", 49);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "notEnhancedLoopWithinEnhancedLoops", 61);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyOuterLoopVariable", 77);
    }

    @Test
    void testGoodEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/GoodEnhanceForLoopUsageCheckTest.class");

        assertNoBugType(MCE_BUG_TYPE);
    }
}
