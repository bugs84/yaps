package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.YapsServlet
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.vertx.java.core.http.HttpServerResponse


class CookiePrefixConfigurationWorks implements VertXTarget, JettyProxy {

    @Override
    String getServletName() {
        "CookiePrefixTestServletName"
    }

    @Override
    void configure(YapsServlet yapsServlet) {
        yapsServlet.setCookiePrefix('MY_PREFIX_${SERVLET_NAME}_SUFFIX')
    }

    @Test(timeout = 1000L)
    void 'cookiePrefix configuration works'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; HttpOnly")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}targetContextPath/index.html").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        def expectedPrefix = "MY_PREFIX_CookiePrefixTestServletName_SUFFIX"
        assert cookieValue == expectedPrefix + "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; HttpOnly"
    }


}
