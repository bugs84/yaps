package cz.vondr.yaps.unit_tests.tool

import org.junit.After
import org.junit.Before
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

trait VertXTarget {


    int targetPort = 25842
    Handler<HttpServerRequest> targetHandler

    boolean targetHandlerWasCalled
    Vertx targetVertx
    HttpServer targetServer

    int getTargetPort() {
        targetPort
    }

    @Before
    void setupATarget() {
        targetHandlerWasCalled = false
        targetVertx = VertxFactory.newVertx()
        targetServer = targetVertx.createHttpServer().requestHandler { req ->
            targetHandlerWasCalled = true
            return targetHandler.handle(req)
        }.listen(targetPort);
    }

    @After
    void tearDownTarget() {
        targetServer.close()
        assert targetHandlerWasCalled, "Something is wrong - target handler wasn't called."
    }

}
