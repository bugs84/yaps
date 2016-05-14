package cz.vondr.yaps.unit_tests

import com.jcabi.http.Response
import com.jcabi.http.request.ApacheRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized.class)
class HttpMethodTest implements VertXTarget, JettyProxy {

    @Parameterized.Parameters
    public static Collection statusCodes() {
        ["GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH", "CONNECT", "MY_OWN_METHOD"]
    }

    String method

    HttpMethodTest(String method) {
        this.method = method
    }

    //TODO fix this test
//    @Ignore("TODO - fix this test - it seems, that here is something wrong with jcabi.")
    @Test(timeout = 1000L)
    void 'http method is resend correctly'() {
        println method
        targetHandler = { req ->
            assert req.method() == method
            req.response().end()
        }

        Response response = new ApacheRequest("$proxyUrl")
                .method(method)
                .fetch()
    }

}
