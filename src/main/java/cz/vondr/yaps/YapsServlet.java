package cz.vondr.yaps;

import yaps.apache.http.Header;
import yaps.apache.http.HttpHost;
import yaps.apache.http.HttpResponse;
import yaps.apache.http.client.methods.CloseableHttpResponse;
import yaps.apache.http.conn.ConnectionKeepAliveStrategy;
import yaps.apache.http.impl.client.CloseableHttpClient;
import yaps.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import yaps.apache.http.impl.client.HttpClients;
import yaps.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import yaps.apache.http.message.BasicHttpEntityEnclosingRequest;
import yaps.apache.http.protocol.HttpContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

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


    //TODO go through and check which headers should be in the list
    //TODO make rewrite it into cleaner way
    //TODO it must ignore case
    protected static final Set<String> OMITED_HEADERS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    static {
        OMITED_HEADERS.add("Connection");
        OMITED_HEADERS.add("Keep-Alive");
        OMITED_HEADERS.add("Proxy-Authenticate");
        OMITED_HEADERS.add("Proxy-Authorization");
        OMITED_HEADERS.add("TE");
        OMITED_HEADERS.add("Trailers");
        OMITED_HEADERS.add("Transfer-Encoding");
        OMITED_HEADERS.add("Upgrade");



//        OMITED_HEADERS.add("Host");
//        OMITED_HEADERS.add("User-Agent");
//        OMITED_HEADERS.add("Accept-Encoding");
//        OMITED_HEADERS.add("Content-Length"); //TODO content lenght to nejak cely rozbije... :-/
    }
    //TODO at redirect 30x (f.e. 302) header "Location" should not be rewrited as well, becouse there will be location header from this proxy (with its own location)

    private void rewriteHeaders(CloseableHttpResponse targetResponse, HttpServletResponse response) {
        for (Header header : targetResponse.getAllHeaders()) {
            String headerName = header.getName();
            //TODO test na velikost pismen
            if (OMITED_HEADERS.contains(headerName)) {
                continue;
            }
            response.addHeader(headerName, header.getValue());
        }
    }

    private void rewriteHeaders(HttpServletRequest request, BasicHttpEntityEnclosingRequest targetRequest) {
        System.out.println("Rewriting response headers:");
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            System.out.println("  "+headerName);
            if (OMITED_HEADERS.contains(headerName)) {
                continue;
            }
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String value = headerValues.nextElement();
//                System.out.println("   " + headerName + ":" + value);
                targetRequest.addHeader(headerName, value);
            }
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
