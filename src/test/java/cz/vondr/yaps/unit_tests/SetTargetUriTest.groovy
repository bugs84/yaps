package cz.vondr.yaps.unit_tests

import cz.vondr.yaps.YapsServlet
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class SetTargetUriTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none()

    YapsServlet yapsServlet

    @Before
    void setup() {
        yapsServlet = new YapsServlet()
    }

    @Test
    void 'set simple uri'() {
        yapsServlet.setTargetUri("http://target.url.cz:12345/context")

        assert yapsServlet.targetSchema == "http"
        assert yapsServlet.targetHost == "target.url.cz"
        assert yapsServlet.targetPort == 12345
        assert yapsServlet.targetUri == "http://target.url.cz:12345/context"
    }

    @Test
    void 'missing port'() {
        yapsServlet.setTargetUri("http://target")

        assert yapsServlet.targetPort == -1
    }

    @Test
    void 'maximal port'() {
        yapsServlet.setTargetUri("http://target:65535")

        assert yapsServlet.targetPort == 65535
    }

    @Test
    void 'invalid url'() {
        expectedException.expect(IllegalArgumentException.class)

        yapsServlet.setTargetUri("http://target:6false")
    }

    @Test
    void 'https schema'() {
        yapsServlet.setTargetUri("https://localhost")

        assert yapsServlet.targetSchema == "https"
    }


    @Test
    void 'fluent interface test'() {
        def yapsFromFluent = yapsServlet.setTargetUri("http://localhost:9000")

        assert yapsServlet.is(yapsFromFluent)
    }
}
