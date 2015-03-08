package cz.vondr.yaps.unit_tests

import cz.vondr.yaps.unit_tests.tool.JettyProxy
import cz.vondr.yaps.unit_tests.tool.VertXTarget
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized.class)
public abstract class StatusCodeTest implements VertXTarget, JettyProxy {

    int statusCode;

    StatusCodeTest(int statusCode) {
        this.statusCode = statusCode
    }

    @Test
    void 'status code is returned correctly'() {
        targetHandler = { req ->
            req.response()
                    .setStatusCode(statusCode)
                    .end()
        }

        CloseableHttpClient httpClient = HttpClients.custom().disableRedirectHandling().build()
        HttpGet httpGet = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpClient.execute(httpGet)

        assert response.getStatusLine().statusCode == statusCode

    }
}
