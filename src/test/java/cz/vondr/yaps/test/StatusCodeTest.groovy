package cz.vondr.yaps.test

import cz.vondr.yaps.test.tool.JettyProxy
import cz.vondr.yaps.test.tool.VertXTarget
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized.class)
public class StatusCodeTest implements VertXTarget, JettyProxy {

    int statusCode;

    StatusCodeTest(int statusCode) {
        this.statusCode = statusCode
    }

    @Parameterized.Parameters
    public static Collection statusCodes() {
        return 200..999
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
