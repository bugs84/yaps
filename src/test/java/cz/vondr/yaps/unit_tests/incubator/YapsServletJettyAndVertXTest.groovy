package cz.vondr.yaps.unit_tests.incubator

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
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

class YapsServletJettyAndVertXTest {

    protected Server proxyServer
    protected int proxyPort
    protected String proxyUrl

    protected Vertx targetVertx;
    protected HttpServer targetServer
    protected int targetPort = 25842
    protected Handler<HttpServerRequest> targetHandler


    @Before
    void setup() {
        setupTarget()
        setupProxy()
    }

    void setupTarget() {
        targetVertx = VertxFactory.newVertx()
        targetServer = targetVertx.createHttpServer().requestHandler { req ->
            return targetHandler.handle(req)
        }.listen(targetPort);
    }

    private void setupProxy() {
        proxyServer = new Server(0)
        ServletHandler handler = new ServletHandler()
        proxyServer.setHandler(handler)

        setupYapsServlet(handler)

        proxyServer.start()

        proxyPort = ((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort()
        proxyUrl = "http://localhost:$proxyPort/"
    }

    protected ServletHolder setupYapsServlet(ServletHandler handler) {
        def servletHolder = new ServletHolder(new YapsServlet().setTargetUri("http://localhost:$targetPort"))
        handler.addServletWithMapping(servletHolder, "/*")
    }

    @After
    void tearDown() {
        proxyServer.stop()
        targetServer.close()
    }

    @Test
    void 'jetty test'() {

        targetHandler = { req ->
            req.response()
                    .putHeader("Content-Type", "text/plain")
                    .setStatusCode(200)
                    .end("Hello World 2");


            int i = 0
        }


        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpGet httpGet = new HttpGet(proxyUrl)
        CloseableHttpResponse response = httpClient.execute(httpGet)



        assert response.getStatusLine().statusCode == 200


        def content = EntityUtils.toString(response.getEntity());

        assert content.size() > 10
        assert content == "Hello World 2"

    }

}
