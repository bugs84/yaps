package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test

/*
Test that some special headers are not copied by proxy
 */

class OmitedHeadersTest implements VertXTarget, JettyProxy {

    @Test(timeout = 1000L)
    void 'omited headers are not resended'() {
        targetHandler = { req ->
            assert req.headers().getAll("Connection") == ["Keep-Alive"]
            assert req.headers().getAll("Keep-Alive") == []
            assert req.headers().getAll("Proxy-Authenticate") == []
            assert req.headers().getAll("Proxy-Authorization") == []
            assert req.headers().getAll("TE") == []
            assert req.headers().getAll("Trailers") == []
            assert req.headers().getAll("Transfer-Encoding") == []
            assert req.headers().getAll("Upgrade") == []
            assert req.headers().getAll("Resend-This-Header") == ["ResendValueReq"]

            req.response()
                    .putHeader("Connection", "Value")
                    .putHeader("Keep-Alive", "Value")
                    .putHeader("Proxy-Authenticate", "Value")
                    .putHeader("Proxy-Authorization", "Value")
                    .putHeader("TE", "Value")
                    .putHeader("Trailers", "Value")
//                    .putHeader("Transfer-Encoding", "Value")
                    .putHeader("Upgrade", "Value")
                    .putHeader("Resend-This-Header", "ResendValueRes")
                    .end()
        }

        Response response = new JdkRequest("$proxyUrl")
                .header("Connection", "Value")
                .header("Keep-Alive", "Value")
                .header("Proxy-Authenticate", "Value")
                .header("Proxy-Authorization", "Value")
                .header("TE", "Value")
                .header("Trailers", "Value")
                .header("Transfer-Encoding", "Value")
                .header("Upgrade", "Value")
                .header("Resend-This-Header", "ResendValueReq")
                .fetch()

        assert response.headers().get("Connection") == null
        assert response.headers().get("Keep-Alive") == null
        assert response.headers().get("Proxy-Authenticate") == null
        assert response.headers().get("Proxy-Authorization") == null
        assert response.headers().get("TE") == null
        assert response.headers().get("Trailers") == null
        assert response.headers().get("Transfer-Encoding") == null
        assert response.headers().get("Upgrade") == null
        assert response.headers().get("Resend-This-Header") == ["ResendValueRes"]

    }
}
