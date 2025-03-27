package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ReadReturnShouldBeCheckedTest extends AbstractIntegrationTest {

    private static final String NCR_BUG_TYPE = "NCR_NOT_CHECKED_READ";

    @Test
    void testNotCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/BadReadReturnShouldBeCheckedTest.class");

        assertBugTypeCount(NCR_BUG_TYPE, 3);

        final String className = "BadReadReturnShouldBeCheckedTest";
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readBytes", 13);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readBytesWithOffset", 22);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readFromBufferedReader", 31);
    }

    @Test
    void testCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/GoodReadReturnShouldBeCheckedTest.class");

        assertNoBugType(NCR_BUG_TYPE);
    }
}
