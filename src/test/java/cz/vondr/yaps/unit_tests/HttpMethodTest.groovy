package cz.vondr.yaps.unit_tests

import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import cz.vondr.yaps.unit_tests.tool.http.HttpClientGenericHttpMethod
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized.class)
class HttpMethodTest implements VertXTarget, JettyProxy {

    @Parameterized.Parameters
    public static Collection statusCodes() {
        //TODO test for "CONNECT" method
        ["GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH", "MY_OWN_METHOD"]
    }

    String method

    HttpMethodTest(String method) {
        this.method = method
    }

    @Test(timeout = 1000L)
    void 'http method is resend correctly'() {
        targetHandler = { req ->
            assert req.method() == method
            req.response().end()
        }

        println "Test for http mehtod '$method'"

        //This is the way how JCabi calls different methods. Unfortunatelly this doesn't work
//        Response response = new ApacheRequest("$proxyUrl")
//                .method(method)
//                .fetch()

        HttpClient client = HttpClientBuilder.create().build();
        def createdUri = URI.create("$proxyUrl")
        HttpClientGenericHttpMethod httpRequest = new HttpClientGenericHttpMethod(createdUri, method);
        HttpResponse response = client.execute(httpRequest);
    }


}
