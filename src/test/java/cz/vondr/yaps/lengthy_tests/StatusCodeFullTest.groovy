package cz.vondr.yaps.lengthy_tests

import cz.vondr.yaps.unit_tests.StatusCodeTest
import org.junit.runners.Parameterized

class StatusCodeFullTest extends StatusCodeTest {
    StatusCodeFullTest(int statusCode) {
        super(statusCode)
    }

    @Parameterized.Parameters
    public static Collection statusCodes() {
        200..999
    }
}
