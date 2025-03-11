package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ReadReturnShouldBeCheckedTest extends AbstractIntegrationTest {

    private static final String RR_BUG_TYPE = "RR_NOT_CHECKED";

    @Test
    void testNotCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/BadReadReturnShouldBeCheckedTest.class");

        assertBugTypeCount(RR_BUG_TYPE, 2);

        final String className = "BadReadReturnShouldBeCheckedTest";
        assertBugInMethodAtLine(RR_BUG_TYPE, className, "readBytes", 12);
        assertBugInMethodAtLine(RR_BUG_TYPE, className, "readBytesWithOffset", 21);
    }

    @Test
    void testCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/GoodReadReturnShouldBeCheckedTest.class");

        assertNoBugType(RR_BUG_TYPE);
    }
}
