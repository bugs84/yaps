package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test

class HostHeaderTest implements VertXTarget, JettyProxy {


    @Test(timeout = 1000L)
    void 'rewrite Host header in request to target address'() {
        targetHandler = { req ->
            //host header is added into request by JCabi - JdkRequest()
            //but it aim into
            //"localhost:$proxyPort". YapsServlet have to rewrite it into
            //"localhost:$targetPort"
            List<String> hostHeaders = req.headers().getAll("Host")
            assert hostHeaders == ["localhost:$targetPort"]
            req.response().end()
        }

        new JdkRequest("$proxyUrl").fetch()
    }

    @Test(timeout = 1000L)
    void 'nothing is done with Host header in Response'() {
        targetHandler = { req ->
            req.response().putHeader("Host", "testHostValue").end()
        }

        Response response = new JdkRequest("$proxyUrl").fetch()

        assert response.headers().get("Host") == ["testHostValue"]

    }

}
