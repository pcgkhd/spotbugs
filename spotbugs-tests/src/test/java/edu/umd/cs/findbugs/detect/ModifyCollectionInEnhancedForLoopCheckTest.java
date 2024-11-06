package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class ModifyCollectionInEnhancedForLoopCheckTest extends AbstractIntegrationTest {

    private static final String MCE_BUG_TYPE = "MCE_MODIFY_COLLECTION_IN_ENHANCED_FOR_LOOP";

    @Test
    void testBadDummyMethodChecks() {
        performAnalysis("enhancedForLoopUsage/BadEnhanceForLoopUsageCheckTest.class");

        assertBugTypeCount(MCE_BUG_TYPE, 4);

        final String className = "BadEnhanceForLoopUsageCheckTest";
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", 7);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyElementDirectly", 11);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyArrayListDirectly", 16);
        assertBugInMethodAtLine(MCE_BUG_TYPE, className, "modifyPrimitiveArray", 19);
    }

    @Test
    void testGoodEndOfFileChecks() {
        performAnalysis("enhancedForLoopUsage/GoodEnhanceForLoopUsageCheckTest.class");

        assertNoBugType(MCE_BUG_TYPE);
    }
}
