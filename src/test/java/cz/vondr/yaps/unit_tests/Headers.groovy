package cz.vondr.yaps.unit_tests

import com.jcabi.http.request.ApacheRequest
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.junit.Test

class Headers implements VertXTarget, JettyProxy {

    @Test(timeout = 1000L)
    void 'header is resend to target'() {
        targetHandler = { req ->
            assert req.headers().get("TestHeader") == "Value of Test header"
            req.response().end()
        }

        new JdkRequest("$proxyUrl").header("TestHeader", "Value of Test header").fetch()
    }

    @Test(timeout = 1000L)
    void 'multiple headers with diacritics are resend to target'() {
        targetHandler = { req ->
            assert req.headers().get("Žluťoučký kůň úpěl ďábelské ódy 1") == "value1"
            assert req.headers().get("key2") == "Žluťoučký kůň úpěl ďábelské ódy 2"
            req.response().end()
        }

        new JdkRequest("$proxyUrl")
                .header("Žluťoučký kůň úpěl ďábelské ódy 1", "value1")
                .header("key2", "Žluťoučký kůň úpěl ďábelské ódy 2")
                .fetch()
    }

    @Test(timeout = 1000L)
    void 'headers with same name is resend to target'() {
        targetHandler = { req ->
            assert req.headers().getAll("TestHeader") == ["Value 1", "Value 2"]
            req.response().end()
        }

        new ApacheRequest("$proxyUrl")
                .header("TestHeader", "Value 1")
                .header("TestHeader", "Value 2")
                .fetch()
    }


}
