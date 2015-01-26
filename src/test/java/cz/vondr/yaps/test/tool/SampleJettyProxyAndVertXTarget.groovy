package cz.vondr.yaps.test.tool

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.junit.Test

class SampleJettyProxyAndVertXTarget implements VertXTarget, JettyProxy {

    @Test
    void 'test response'() {
        targetHandler = { req ->
            req.response()
                    .putHeader("Content-Type", "text/plain")
                    .setStatusCode(200)
                    .end("Hello World 2");
        }

        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpGet httpget = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpclient.execute(httpget)

        assert response.getStatusLine().statusCode == 200
        def content = EntityUtils.toString(response.getEntity());
        assert content.size() > 10
        assert content == "Hello World 2"
    }

    @Test
    void 'test request'() {
        targetHandler = { req ->
            assert req.method() == "GET"
            req.response().end()
        }

        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpGet httpGet = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpClient.execute(httpGet)
    }

}
