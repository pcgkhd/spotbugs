package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindEnhancedForLoopVariableModificationTest extends AbstractIntegrationTest {

    private static final String MEV_BUG_TYPE = "MEV_MODIFY_ENHANCED_FOR_LOOP_VARIABLE";

    @Test
    void testBadEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/BadFindEnhancedForLoopVariableModificationTest.class");

        assertBugTypeCount(MEV_BUG_TYPE, 18);

        final String className = "BadFindEnhancedForLoopVariableModificationTest";
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyCollectionWithEnhancedForLoop", "i", 15);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyStringElement", "name", 30);
        assertBugAtVar(MEV_BUG_TYPE, className, "loopWithinLoop", "number", 39);
        assertBugAtVar(MEV_BUG_TYPE, className, "notEnhancedLoopWithinEnhancedLoops", "number", 50);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyOuterLoopVariable", "numbers", 65);
        assertBugAtVar(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", "number", 74);
        assertBugAtVar(MEV_BUG_TYPE, className, "multipleEnhancedLoopVariableModification", "numbers", 76);
        assertBugAtVar(MEV_BUG_TYPE, className, "arrayEnhancedForLoop", "number", 84);
        assertBugAtVar(MEV_BUG_TYPE, className, "charArrayEnhancedForLoop", "character", 92);
        assertBugAtVar(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", "character", 104);
        assertBugAtVar(MEV_BUG_TYPE, className, "mixedForLoopsWithEnhancedLoops", "number", 106);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyOuterEnhancedLoopVariable", "outer", 117);
        assertBugAtVar(MEV_BUG_TYPE, className, "arrayEnhancedLoopWithConversionsAndMethodCalls", "d", 125);
        assertBugAtVar(MEV_BUG_TYPE, className, "collectionEnhancedLoopWithMethodCall", "item", 133);
        assertBugAtVar(MEV_BUG_TYPE, className, "unclearIntentWithForeach", "num", 143);
        assertBugAtVar(MEV_BUG_TYPE, className, "unclearIntentWithForeach", "num", 148);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyElementWithBreak", "num", 160);
        assertBugAtVar(MEV_BUG_TYPE, className, "modifyElementWithContinue", "num", 171);
    }

    @Test
    void testGoodEnhancedForLoopUsageChecks() {
        performAnalysis("enhancedForLoopUsage/GoodFindEnhancedForLoopVariableModificationTest.class", "enhancedForLoopUsage/Person.class");

        assertNoBugType(MEV_BUG_TYPE);
    }
}
