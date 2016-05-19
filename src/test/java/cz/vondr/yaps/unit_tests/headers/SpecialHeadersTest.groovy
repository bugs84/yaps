package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.vertx.java.core.http.HttpServerResponse

class SpecialHeadersTest implements VertXTarget, JettyProxy {

    @Test(timeout = 1000L)
    void 'nothing is done with User-Agent header'() {
        targetHandler = { req ->
            assert req.headers().get("User-Agent") == "User agent request value"
            HttpServerResponse response = req.response()
            response.headers().add("User-Agent", "User agent response value")
            response.end()
        }

        Response response = new JdkRequest("$proxyUrl").header("User-Agent", "User agent request value").fetch()
        assert response.headers().get("User-Agent") == ["User agent response value"]
    }
}
