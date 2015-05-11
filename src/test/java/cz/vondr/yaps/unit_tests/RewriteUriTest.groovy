package cz.vondr.yaps.unit_tests

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Ignore
import org.junit.Test

class RewriteUriTest implements VertXTarget, JettyProxy {

    @Test
    @Ignore
    void 'path is rewritten just writing test'() {
        targetHandler = { req ->
            println "AAAAAAAAA"
            println req.absoluteURI()
            println req.path()
            println req.uri()
            println req.method()
            println req.query()
            println req.version()
            println req.remoteAddress()
            println req.localAddress()
            println req.params()
            println req.headers()

            req.response().end()
        }

        new JdkRequest("${proxyUrl}/context/path.txt?param1&foo=bar#fragment").fetch()

    }

    @Test(timeout = 1000L)
    void 'not empty path'() {
        targetHandler = { req ->
            assert req.path() == "/testing/path/to/file.jfe"
            assert req.uri() == "/testing/path/to/file.jfe"
            req.response().setStatusCode(204).end()
        }

        Response response = new JdkRequest("$proxyUrl/testing/path/to/file.jfe").fetch()

        assert response.status() == 204
    }

    @Test(timeout = 1000L)
    void 'empty path without slash in the end'() {
        proxyUrl = "http://localhost:$proxyPort"
        targetHandler = { req ->
            assert req.path() == "/"
            req.response().end()
        }

        new JdkRequest("$proxyUrl").fetch()
    }

    @Test(timeout = 1000L)
    void 'empty path with slash in the end'() {
        proxyUrl = "http://localhost:$proxyPort/"
        targetHandler = { req ->
            assert req.path() == "/"
            req.response().end()
        }

        new JdkRequest("$proxyUrl").fetch()
    }

    @Test(timeout = 1000L)
    void 'empty query string is rewritten correctly'() {
        targetHandler = { req ->
            assert req.query() == null
            req.response().end()
        }

        new JdkRequest("$proxyUrl").fetch()
    }


    @Test(timeout = 1000L)
    void 'path with query'() {
        targetHandler = { req ->
            assert req.path() == "/context/path.txt"
            req.response().end()
        }

        new JdkRequest("$proxyUrl/context/path.txt?param1=1&novalue;foo=bar#fragment").fetch()
    }

    @Test(timeout = 1000L)
    void 'query string is rewritten'() {
        targetHandler = { req ->
            assert req.query() == "param1=1&noValue;foo=bar"
            assert req.params().asList().collectEntries {
                [(it.key): it.value]
            } == ["param1": "1", "noValue": "", "foo": "bar"]
            req.response().end()
        }

        new JdkRequest("$proxyUrl/context/path.txt?param1=1&noValue;foo=bar#fragment").fetch()
    }

    @Test(timeout = 1000L)
    void 'uri is rewritten'() {
        targetHandler = { req ->
            assert req.uri() == "/context/path.txt?param1=1&noValue;foo=bar"
            req.response().end()
        }

        new JdkRequest("$proxyUrl/context/path.txt?param1=1&noValue;foo=bar#fragment").fetch()
    }


}