package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.YapsServlet
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test
import org.vertx.java.core.http.HttpServerResponse

public class CookiesTest implements VertXTarget, JettyProxy {

    @Override
    String getProxyContextPath() {
        "/proxyContextPath/"
    }

    @Override
    String getTargetContextPath() {
        "/targetContextPath"
    }

    @Override
    void configure(YapsServlet yapsServlet) {
        yapsServlet.setCookiePrefix("")
    }

    @Test(timeout = 1000L)
    void 'header is resend to target'() {
        targetHandler = { req ->
            assert req.headers().get("TestHeader") == "Value of Test header"
            HttpServerResponse response = req.response()
            response.end()

        }
        new JdkRequest("$proxyUrl").header("TestHeader", "Value of Test header").fetch()
    }

    @Test(timeout = 1000L)
    void 'set cookie HttpOnly flag is resend in response'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; Path=/contextPath/; HttpOnly")
            response.end()
        }
        Response response = new JdkRequest("${proxyUrl}context/aa").fetch()

        def cookieHeaderValue = response.headers().get("Set-Cookie")[0]
        assert cookieHeaderValue.contains("; HttpOnly")
    }

    @Test(timeout = 1000L)
    void 'cookie context path is rewritten to proxy context path'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; path=/targetContextPath; HttpOnly")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}targetContextPath/index.html").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("path=/proxyContextPath")

        assert cookieValue == "JSESSIONID=82AD861ECDA4F81467924ACD2687BE11; path=/proxyContextPath; HttpOnly"
    }

    @Test(timeout = 1000L)
    void 'cookie context path is rewritten to proxy context path even when path is in the end'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "a=b; path=/targetContextPath")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}targetContextPath/index.html").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("path=/proxyContextPath")

        assert cookieValue == "a=b; path=/proxyContextPath"
    }

    @Test(timeout = 1000L)
    void 'cookie context path is rewritten to proxy context path and works case insensitively'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "a=b; paTH=/targetContextPath")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}targetContextPath/index.html").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("paTH=/proxyContextPath")
    }

    @Test(timeout = 1000L)
    void 'domain is removed from cookie'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "CN=Cv; path=/targetContextPath; HttpOnly; domain=example.com")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("path=/proxyContextPath")

        assert cookieValue == "CN=Cv; path=/proxyContextPath; HttpOnly"
    }

    @Test(timeout = 1000L)
    void 'domain is removed from cookie and works case insensitive'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "CN=Cv; HttpOnly; DoMAiN=example.com; path=/targetContextPath;")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("path=/proxyContextPath")

        assert cookieValue == "CN=Cv; HttpOnly; path=/proxyContextPath;"
    }

    @Test(timeout = 1000L)
    void 'domain is removed from cookie even when domain is empty'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "CN=Cv; Domain=; path=/targetContextPath;")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}").fetch()

        def cookieValue = response.headers().get("Set-Cookie")[0]
        assert cookieValue.contains("path=/proxyContextPath")

        assert cookieValue == "CN=Cv; path=/proxyContextPath;"
    }


    @Test(timeout = 1000L)
    void 'multiple SetCookie headers works'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            //set two headers Set-Cookie
            response.headers().add("Set-Cookie", "aN=av; path=/targetContextPath; HttpOnly")
            response.headers().add("Set-Cookie", "B=BB; path=/targetContextPath; Secure; Version=1")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}").fetch()

        assert response.headers().get("Set-Cookie").size() == 2

        def cookie1Value = response.headers().get("Set-Cookie")[0]
        assert cookie1Value == "B=BB; path=/proxyContextPath; Secure; Version=1"

        def cookie2Value = response.headers().get("Set-Cookie")[1]
        assert cookie2Value == "aN=av; path=/proxyContextPath; HttpOnly"
//        proxyServer.join()
    }


    @Test(timeout = 1000L)
    void 'SetCookie with multiple cookies separated by comma RFC2109'() {
        targetHandler = { req ->
            HttpServerResponse response = req.response()
            response.putHeader("Set-Cookie", "a=b;Path=/targetContextPath;version=1;,  c=d;Path=/targetContextPath;")
            response.end()
        }

        Response response = new JdkRequest("${proxyUrl}").fetch()

        assert response.headers().get("Set-Cookie").size() == 1

        def cookie1Value = response.headers().get("Set-Cookie")[0]
        assert cookie1Value == "a=b;Path=/proxyContextPath;version=1;,  c=d;Path=/proxyContextPath;"
    }

}
