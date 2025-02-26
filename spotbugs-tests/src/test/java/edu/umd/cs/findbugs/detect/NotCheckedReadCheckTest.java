package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class NotCheckedReadCheckTest extends AbstractIntegrationTest {

    private static final String NCR_BUG_TYPE = "NCR_NOT_CHECKED_READ";

    @Test
    void testBadNotCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/BadPartialFilledArrayWithRead.class");

        assertBugTypeCount(NCR_BUG_TYPE, 1);

        final String className = "BadPartialFilledArrayWithRead";
        assertBugAtLine(NCR_BUG_TYPE, 11);
    }
}
