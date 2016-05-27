package cz.vondr.yaps.unit_tests.headers

import com.jcabi.http.Response
import com.jcabi.http.request.JdkRequest
import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.apache.http.Header
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.junit.Ignore
import org.junit.Test

class HeadersTest implements VertXTarget, JettyProxy {

    @Test(timeout = 1000L)
    void 'header is resend to target'() {
        targetHandler = { req ->
            assert req.headers().get("TestHeader") == "Value of Test header"
            req.response().end()
        }

        new JdkRequest("$proxyUrl").header("TestHeader", "Value of Test header").fetch()
    }

    @Test(timeout = 1000L)
    @Ignore("Not ascii characters in http headers are not guaranteed to work")
    void 'multiple headers with diacritics are resend to target'() {
        targetHandler = { req ->
            assert req.headers().get("Žluťoučký kůň úpěl ďábelské ódy 1") == "value1"
            assert req.headers().get("headerName2") == "Žluťoučký kůň úpěl ďábelské ódy 2"
            req.response().end()
        }

        new JdkRequest("$proxyUrl")
                .header("Žluťoučký kůň úpěl ďábelské ódy 1", "value1")
                .header("headerName2", "Žluťoučký kůň úpěl ďábelské ódy 2")
                .fetch()
    }

    @Test(timeout = 1000L)
    void 'headers with same name are resend to target'() {
        targetHandler = { req ->
            assert req.headers().getAll("TestHeader") == ["Value 1", "Value 2"]
            req.response().end()
        }

        new JdkRequest(proxyUrl)
                .header("TestHeader", "Value 1")
                .header("TestHeader", "Value 2")
                .fetch()
    }

    @Test(timeout = 1000L)
    void 'header is resend from response'() {
        targetHandler = { req ->
            def response = req.response()
            response.headers().add("TestResponseHeader", "Value of testing header.")
            response.end()
        }

        def responseHeaders = new JdkRequest("$proxyUrl").fetch().headers()
        assert responseHeaders.get("TestResponseHeader") == ["Value of testing header."]
    }

    @Test()
    void 'multiple headers are resend from response'() {
        targetHandler = { req ->
            def response = req.response()
            response.headers()
                    .add("Header_key_1", "value  one")
                    .add("second_header_key", "ValuE 2")

            response.end("", "UTF-8")
        }

        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpGet httpGet = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpClient.execute(httpGet)

        assertResponseContainsHeader(response, "Header_key_1", "value  one")
        assertResponseContainsHeader(response, "second_header_key", "ValuE 2")
    }

    private void assertResponseContainsHeader(CloseableHttpResponse response, String headerKey, String headerValue) {
        def header1 = response.getHeaders(headerKey)
        assert header1.size() == 1
        assert header1[0].name == headerKey && header1[0].value == headerValue
    }

    @Ignore("Bug in JdkRequest - reported - https://github.com/jcabi/jcabi-http/issues/81")
    @Test()
    void 'bug in JdkRequest'() {
        targetHandler = { req ->
            def response = req.response()
            response.headers()
                    .add("Header_key_1", "value  one")
                    .add("second_header_key", "ValuE 2")

            response.end("", "UTF-8")
        }


        def request = new JdkRequest("$proxyUrl")
        Response response = request.fetch()
        def responseHeaders = response.headers()
        assert responseHeaders.get("Header_key_1") == ["value  one"]

        //there is no header "second_header_key" but there is header "Second_header_key"
        //This capitalize header... But WHY????
        //This is issue in JdkRequest
        assert responseHeaders.get("second_header_key") == ["ValuE 2"]
    }

    @Test()
    void 'response headers with different case sensitivity'() {
        targetHandler = { req ->
            def response = req.response()
            response.headers()
                    .add("Header_key_1", "value  one")
                    .add("second_header_key", "ValuE 2")

            response.end("", "UTF-8")
        }

        //apache http client must be used for this test, because JCabi change case sensitivity of headers :(
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("$proxyUrl");

        CloseableHttpResponse response = client.execute(get)


        Header[] firstHeader = response.getHeaders("Header_key_1")
        assert firstHeader.size() == 1
        assert firstHeader[0].name == "Header_key_1"
        assert firstHeader[0].value == "value  one"
        Header[] secondHeader = response.getHeaders("second_header_key")
        assert secondHeader.size() == 1
        assert secondHeader[0].name == "second_header_key"
        assert secondHeader[0].value == "ValuE 2"
    }

    @Test(timeout = 1000L)
    void 'multiple headers with the same name are resend from request'() {
        targetHandler = { req ->
            List<String> testHeaderValues = req.headers().getAll("TestHeader")
            assert testHeaderValues.size() == 2
            assert testHeaderValues[0] == "Value 1"
            assert testHeaderValues[1] == "Value 2"
            req.response().end()
        }

        new JdkRequest("$proxyUrl")
                .header("TestHeader", "Value 1")
                .header("TestHeader", "Value 2")
                .fetch().headers()
    }

    @Test(timeout = 1000L)
    void 'multiple headers with the same name are resend from response'() {
        targetHandler = { req ->
            def response = req.response()
            response.headers()
                    .add("TestHeader", "Value 1")
                    .add("TestHeader", "Value 2")
            response.end()
        }

        def responseHeaders = new JdkRequest("$proxyUrl").fetch().headers()
        assert responseHeaders.get("TestHeader") as Set == ["Value 1", "Value 2"] as Set
    }

}
