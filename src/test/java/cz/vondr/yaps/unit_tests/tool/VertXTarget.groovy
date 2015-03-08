package cz.vondr.yaps.unit_tests.tool
import org.junit.After
import org.junit.Before
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

trait VertXTarget {


    Vertx targetVertx;
    HttpServer targetServer
    int targetPort = 25842
    Handler<HttpServerRequest> targetHandler

    int getTargetPort() {
        targetPort
    }

    @Before
    void setupATarget() {
        targetVertx =  VertxFactory.newVertx()
        targetServer = targetVertx.createHttpServer().requestHandler { req ->
            return targetHandler.handle(req)
        }.listen(targetPort);
    }

    @After
    void tearDownTarget() {
        targetServer.close()

    }

}
