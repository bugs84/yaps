package cz.vondr.yaps;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

public class YapsServlet extends HttpServlet {
    private static final char QUERY_DELIMITER = '?';

    //Note: Its possible to improve performance using Java 7 Channels

    protected CloseableHttpClient httpClient;

    protected String targetUri = "http://localhost:8080";

    protected String targetSchema = "http";
    protected String targetHost = "twitter.com";
    protected int targetPort = 80;
    protected HttpHost targetHttpHost = new HttpHost(targetHost, targetPort);


    public String getTargetSchema() {
        return targetSchema;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getTargetUri() {
        return targetUri;
    }

    /** for example "http://localhost:8080/context" */
    public YapsServlet setTargetUri(String targetUri) {
        this.targetUri = targetUri;
        URI uri = parseUri(targetUri);
        targetSchema = uri.getScheme();
        targetHost = uri.getHost();
        targetPort = uri.getPort();
        targetHttpHost = new HttpHost(targetHost, targetPort, targetSchema);
        return this;
    }

    private URI parseUri(String targetUri) {
        try {
            return new URI(targetUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        createHttpClient();
    }

    private void createHttpClient() {

        ConnectionKeepAliveStrategy keepAliveStrategy = new DefaultConnectionKeepAliveStrategy() {

            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    // Keep connections alive 5 seconds if a keep-alive value
                    // has not be explicitly set by the server
                    keepAlive = 5000; //TODO parameterize
                }
                return keepAlive;
            }

        };


        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200); //TODO parameterize
        connectionManager.setDefaultMaxPerRoute(20); //TODO parameterize

        //TODO find optimal client setup
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableCookieManagement()
                .setKeepAliveStrategy(keepAliveStrategy)
                .disableRedirectHandling()
                .build();

        //TODO read about  http.protocol.expect-continue http.protocol.expect-continue
        //TODO read about  http.connection.stalecheck

    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //TODO unit_tests for https  (setup jetty with https)
        String method = request.getMethod();
        String rewrittenTargetUri = rewriteUri(request);

        BasicHttpEntityEnclosingRequest targetRequest = new BasicHttpEntityEnclosingRequest(method, rewrittenTargetUri);
        rewriteHeaders(request, targetRequest);
        CloseableHttpResponse targetResponse = httpClient.execute(targetHttpHost, targetRequest);
        try {
            //TODO rewrite 'target response' to 'proxy response'
            response.setStatus(targetResponse.getStatusLine().getStatusCode());
            rewriteHeaders(targetResponse, response);
            //TODO test for rewriting body - from proxy request to target request and from target response to proxy response
            targetResponse.getEntity().writeTo(response.getOutputStream());
        } finally {
            targetResponse.close();
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }


        //TODO multi-part

    }

    private void rewriteHeaders(CloseableHttpResponse targetResponse, HttpServletResponse response) {
        for (Header header : targetResponse.getAllHeaders()) {
            response.addHeader(header.getName(), header.getValue());
        }
    }

    private void rewriteHeaders(HttpServletRequest request, BasicHttpEntityEnclosingRequest targetRequest) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            targetRequest.addHeader(headerName, headerValue);
        }

    }

    private String rewriteUri(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder();//TODO init String builder size
        uri.append(targetUri);
        uri.append(request.getPathInfo());//TODO path info nefunguje spravne na WAS - napsat unit a upravit
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri.append(QUERY_DELIMITER).append(queryString);
        }
        return uri.toString();
        //TODO test na uri s diakritikou

        //TODO Zajistit aby na konci target uri bylo/nebylo lomeno "/"
    }

}
