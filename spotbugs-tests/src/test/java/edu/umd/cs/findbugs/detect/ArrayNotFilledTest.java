package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ArrayNotFilledTest extends AbstractIntegrationTest {

    private static final String ANF_BUG_TYPE = "ANF_ARRAY_MIGHT_NOT_BE_FILLED";

    @Test
    void testBadNotCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/BadArrayNotFilled.class");

        assertBugTypeCount(ANF_BUG_TYPE, 1);

        assertBugAtLine(ANF_BUG_TYPE, 12);
    }
}
