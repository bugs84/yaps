package cz.vondr.yaps.unit_tests

import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Ignore
import org.junit.Test

class RewriteBodyTest implements VertXTarget, JettyProxy {

    //TODO finish this test
    @Ignore("TODO - finish this test")
    @Test(timeout = 1000L)
    void 'Rewrite simple body from request'() {
        targetHandler = { req ->
            println "HANDLER"
            def response = req.response()
            response.headers().add("TestResponseHeader", "Value of testing header.")
            response.setStatusCode(204)
            response.end()
        }

//        def response = new ApacheRequest("$proxyUrl")
//                .method("POST")
////                .body().set("Simple body").back()
//                .body().formParam("key","val").back()
//                .fetch()

//        Response response = new ApacheRequest("$proxyUrl")
//                        .method("POST")
//                        .fetch()

        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpPost httpget = new HttpPost(proxyUrl + "/ahoj")
        CloseableHttpResponse response = httpclient.execute(httpget)

//        assert response.status() == 204

        int i = 0
    }

}
