package cz.vondr.yaps.unit_tests.tool

import cz.vondr.yaps.YapsServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.After
import org.junit.Before

trait JettyProxy {

    Server proxyServer
    int proxyPort
    String proxyUrl

    abstract int getTargetPort()

    public String getProxyContextPath() {
        "/"
    }

    public String getTargetContextPath() {
        ""
    }

    @Before
    void setupProxy() {
        proxyServer = new Server(0)
        ServletContextHandler handler = new ServletContextHandler()
        setupYapsServlet(handler)
        proxyServer.setHandler(handler)

        proxyServer.start()

        proxyPort = ((ServerConnector) proxyServer.getConnectors()[0]).getLocalPort()
        proxyUrl = "http://localhost:$proxyPort${getProxyContextPath()}"
        println "Proxy is running '$proxyUrl'"
    }

    private void setupYapsServlet(ServletContextHandler handler) {
        def servletHolder = new ServletHolder(new YapsServlet().setTargetUri("http://localhost:${getTargetPort()}${getTargetContextPath()}"))
        handler.setContextPath(getProxyContextPath())
        handler.addServlet(servletHolder, "/*")
    }

    @After
    void tearDownProxy() {
        proxyServer.stop()
    }

}