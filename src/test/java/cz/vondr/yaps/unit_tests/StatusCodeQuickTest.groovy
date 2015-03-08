package cz.vondr.yaps.unit_tests

import org.junit.runners.Parameterized

class StatusCodeQuickTest extends StatusCodeTest {
    StatusCodeQuickTest(int statusCode) {
        super(statusCode)
    }

    @Parameterized.Parameters
    public static Collection statusCodes() {
        [200, 204, 302, 307, 400, 404, 500]
    }
}
