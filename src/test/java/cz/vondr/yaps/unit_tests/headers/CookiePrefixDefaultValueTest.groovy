package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.vertx.java.core.http.HttpServerResponse

class CookiePrefixDefaultValueTest implements VertXTarget, JettyProxy {

    @Override
    String getServletName() {
        "CookiePrefixTestServletName"
    }

    @Test(timeout = 1000L)
    void 'cookie name is rewritten _ expected prefix is added'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; HttpOnly")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}targetContextPath/index.html").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        def expectedPrefix = "!yaps!CookiePrefixTestServletName!"
        assert cookieValue == expectedPrefix + "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; HttpOnly"
    }


}
