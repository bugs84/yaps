package cz.vondr.yaps.unit_tests.tool.examples

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.vertx.java.core.http.HttpServerResponse

class JettyProxyContextPathExampleTest implements VertXTarget, JettyProxy {

    @Override
    String getProxyContextPath() {
        "/proxy/"
    }

    @Override
    String getTargetContextPath() {
        "/target"
    }

    @Test(timeout = 1000L)
    void 'proxy and target have its context paths'() {
        targetHandler = { req ->
            req.absoluteURI()
            assert req.path() == "/target/index.html"
            HttpServerResponse response = req.response()
            response.end()
        }

        assert proxyUrl.contains("/proxy/")
        Response response = new JdkRequest("${proxyUrl}index.html").fetch()

        assert response.status() == 200
    }

    @Test(timeout = 1000L)
    void 'request for proxy without context return 404'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.end()
        }

        assert proxyUrl.contains("/proxy/")
        def proxyUrlWithoutContext = proxyUrl.replace("proxy/", "")
        Response response = new JdkRequest("$proxyUrlWithoutContext").fetch()

        assert response.status() == 404
        new JdkRequest("$proxyUrl").fetch()
    }
}
