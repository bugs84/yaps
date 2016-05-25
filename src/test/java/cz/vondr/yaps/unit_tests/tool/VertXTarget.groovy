package cz.vondr.yaps.unit_tests.tool

import cz.vondr.yaps.unit_tests.tool.http.SocketUtil
import org.junit.After
import org.junit.Before
import org.vertx.java.core.Handler
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.http.HttpServer
import org.vertx.java.core.http.HttpServerRequest

import java.util.concurrent.CountDownLatch

trait VertXTarget {


    int targetPort = -1
    Handler<HttpServerRequest> targetHandler

    boolean targetHandlerWasCalled
    Vertx targetVertx
    HttpServer targetServer

    int getTargetPort() {
        initTargetPort()
        targetPort
    }

    private synchronized void initTargetPort() {
        if (targetPort < 0) {
            targetPort = SocketUtil.findFreePort()
        }
    }

    @Before
    void setupATarget() {
        targetHandlerWasCalled = false
        initTargetPort()
        targetVertx = VertxFactory.newVertx()
        targetServer = targetVertx.createHttpServer().requestHandler { req ->
            targetHandlerWasCalled = true
            return targetHandler.handle(req)
        }
        startTargetServer(targetServer)
    }

    private void startTargetServer(HttpServer targetServer) {
        CountDownLatch serverRunningSignal = new CountDownLatch(1);
        targetServer.listen(getTargetPort(), { event ->
            println "Testing target is runnig 'http://localhost:$targetPort'"
            serverRunningSignal.countDown()
        });
        serverRunningSignal.await()
    }

    @After
    void tearDownTarget() {
        targetServer.close()
        assert targetHandlerWasCalled, "Something is wrong - target handler wasn't called."
    }

}
