package cz.vondr.yaps

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
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.http.HttpClientResponse

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

    @Test
    void 'vertx server'() {
        def vertx = VertxFactory.newVertx()
        vertx.createHttpServer().requestHandler { req ->
            req.response().headers().set("Content-Type", "text/plain");
            req.response().end("Hello World");
        }.listen(8080);



        httpComponentsHttpRequest()


        vertXClientHttpRequest(vertx)




        vertx.stop()

        int i = 0
    }

    private void httpComponentsHttpRequest() {
        //HttpComponents - request
        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpGet httpget = new HttpGet("http://localhost:8080/")
        CloseableHttpResponse response = httpclient.execute(httpget)

        def content = EntityUtils.toString(response.getEntity());
        assert content == "Hello World"
    }

    private void vertXClientHttpRequest(Vertx vertx) {
        //VertX Client - request
        def client = vertx.createHttpClient().setHost("localhost").setPort(8080)
        client.request("GET", "trantada", new Handler<HttpClientResponse>() {
            @Override
            void handle(HttpClientResponse resp) {
                println "VertX - ${resp.statusCode()}"
                resp.bodyHandler(new Handler<Buffer>() {
                    @Override
                    void handle(Buffer buffer) {
                        def content = buffer.toString()
                        assert content == "Hello World"
                        println "VertX - $content"
                    }
                })

            }
        }).end()

        //                    client.request("GET", "") { resp ->
        //                        println "Got a response: ${resp.statusCode}"
        //                    }
        //                    .end()
        Thread.sleep(2000)

        //        System.in.read(new byte[1])
    }


}
