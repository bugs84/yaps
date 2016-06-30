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

    /** Can be overridden in test to specify servlet name */
    public String getServletName() {
        "yaps-test"
    }

    /** Can be overridden in test to specify proxy context path */
    public String getProxyContextPath() {
        "/"
    }

    /** Can be overridden in test to specify target context path */
    public String getTargetContextPath() {
        ""
    }

    /** Can be overridden in test to do additional configuration to yaps servlet */
    public void configure(YapsServlet yapsServlet) {
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
        def servletHolder = new ServletHolder(getServletName(), createYapsServlet())
        handler.setContextPath(getProxyContextPath())
        handler.addServlet(servletHolder, "/*")
    }

    public YapsServlet createYapsServlet() {
        def yapsServlet = new YapsServlet()
        yapsServlet.setTargetUri("http://localhost:${getTargetPort()}${getTargetContextPath()}")
        configure(yapsServlet)
        yapsServlet
    }

    @After
    void tearDownProxy() {
        proxyServer.stop()
    }

}