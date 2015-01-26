package cz.vondr.yaps.test.incubator

import cz.vondr.yaps.YapsServlet
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.After
import org.junit.Before
import org.junit.Test

class YapsServletTestWithEmbeddedJettyTest {

    protected Server proxyServer
    protected int proxyPort
    protected String proxyUrl

    @Before
    void setup() {
        proxyServer = new Server(0)
        ServletHandler handler = new ServletHandler()
        proxyServer.setHandler(handler)

        setupYapsServlet(handler)

        proxyServer.start()

        proxyPort = ((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort()
        proxyUrl = "http://localhost:$proxyPort/"

    }

    protected ServletHolder setupYapsServlet(ServletHandler handler) {
        handler.addServletWithMapping(YapsServlet.class, "/*")
    }

    @After
    void tearDown() {
        proxyServer.stop()

    }

    @Test
    void 'jetty test'() {


        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpGet httpget = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpclient.execute(httpget)



        assert response.getStatusLine().statusCode == 200


        def content = EntityUtils.toString(response.getEntity());

        assert content.size() > 10

    }

}
