package cz.vondr.yaps.test.tool

import cz.vondr.yaps.YapsServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.After
import org.junit.Before

trait JettyProxy {

    Server proxyServer
    int proxyPort
    String proxyUrl

    @Before
    void setupProxy() {
        proxyServer = new Server(0)
        ServletHandler handler = new ServletHandler()
        proxyServer.setHandler(handler)

        setupYapsServlet(handler)

        proxyServer.start()

        proxyPort = ((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort()
        proxyUrl = "http://localhost:$proxyPort/"
    }

    private ServletHolder setupYapsServlet(ServletHandler handler) {
        def servletHolder = new ServletHolder(new YapsServlet().setTargetHost("localhost").setTargetPort(targetPort))
        handler.addServletWithMapping(servletHolder, "/*")
    }

    @After
    void tearDownProxy() {
        proxyServer.stop()
    }

}