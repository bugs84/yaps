package cz.vondr.yaps.test

import com.jcabi.http.Response
import com.jcabi.http.request.ApacheRequest
import cz.vondr.yaps.test.tool.JettyProxy
import cz.vondr.yaps.test.tool.VertXTarget
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

    @Test(timeout = 1000L)
    void 'http method is resend correctly'() {
        targetHandler = { req ->
            assert req.method() == method
            req.response()
                    .setStatusCode(204)
                    .end();
        }

        Response response = new ApacheRequest("$proxyUrl")
                .method(method)
                .fetch()
    }

}
