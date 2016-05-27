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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class YapsServlet extends HttpServlet {
    private static final char QUERY_DELIMITER = '?';
    private static final String HOST_HEADER = "Host";

    private static final String COOKIE_HEADER = "Cookie";
    private static final String COOKIE2_HEADER = "Cookie2";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String SET_COOKIE2_HEADER = "Set-Cookie2";


    private static final Set<String> OMITED_HEADERS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    static {
        //viz. https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
        //End-to-end and Hop-by-hop Headers
        OMITED_HEADERS.add("Connection");
        OMITED_HEADERS.add("Keep-Alive");
        OMITED_HEADERS.add("Proxy-Authenticate");
        OMITED_HEADERS.add("Proxy-Authorization");
        OMITED_HEADERS.add("TE");
        OMITED_HEADERS.add("Trailers");
        OMITED_HEADERS.add("Transfer-Encoding");
        OMITED_HEADERS.add("Upgrade");

        //other
        // todo content lenght - solve content-length - mozna ze ji bude nastavovat inputStream nebo tak
        //TODO vyzkouset a napsat test, ze content-length se prepisuje spravne tam i zpet
//        OMITED_HEADERS.add("Content-Length"); //TODO content lenght to nejak cely rozbije... :-/
    }
    //TODO at redirect 30x (f.e. 302) header "Location" should not be rewrited as well, becouse there will be location header from this proxy (with its own location)


    //Note: Its possible to improve performance using Java 7 Channels

    protected CloseableHttpClient httpClient;

    protected String targetUri = "http://www.idnes.cz";

    protected String targetSchema = "http";
    protected String targetHost = "www.idnes.cz";//todo target host
    protected int targetPort = 80;//-1 if port is undefined
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

    /**
     * for example "http://localhost:8080/context"
     */
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
        try {
            //TODO unit_tests for https  (setup jetty with https)
            String method = request.getMethod();
            String rewrittenTargetUri = rewriteUri(request);

            BasicHttpEntityEnclosingRequest targetRequest = new BasicHttpEntityEnclosingRequest(method, rewrittenTargetUri);
            rewriteRequestHeaders(request, targetRequest);
            CloseableHttpResponse targetResponse = httpClient.execute(targetHttpHost, targetRequest);
            try {
                //TODO rewrite 'target response' to 'proxy response'
                response.setStatus(targetResponse.getStatusLine().getStatusCode());
                rewriteResponseHeaders(request, targetResponse, response);
                //TODO test for rewriting body - from proxy request to target request and from target response to proxy response
                targetResponse.getEntity().writeTo(response.getOutputStream());
            } finally {
                //TODO mozna zavolat neco jako EntityUtils.consume(targetResponse.getEntity())
                targetResponse.close();
                response.getOutputStream().flush();
                response.getOutputStream().close();
            }

        } catch (RuntimeException e) {
            //TODO log exception
            e.printStackTrace();
            throw e;
        }

        //TODO multi-part

    }

    private void rewriteRequestHeaders(HttpServletRequest request, BasicHttpEntityEnclosingRequest targetRequest) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            //TODO solve content-lenght header
            if (OMITED_HEADERS.contains(headerName)) {
                continue;
            } else if (HOST_HEADER.equalsIgnoreCase(headerName)) {
                if (targetPort == -1) {
                    targetRequest.addHeader(headerName, targetHost);
                } else {
                    targetRequest.addHeader(headerName, targetHost + ":" + targetPort);
                }
            } else {
                Enumeration<String> headerValues = request.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {

                    if (COOKIE_HEADER.equalsIgnoreCase(headerName)) {
                        //TODO rewrite cookie
                        //TODO COOKIE2_HEADER
                    }

                    String value = headerValues.nextElement();
                    targetRequest.addHeader(headerName, value);
                }
            }
        }
    }

    private void rewriteResponseHeaders(HttpServletRequest request, CloseableHttpResponse targetResponse, HttpServletResponse response) {
        for (Header header : targetResponse.getAllHeaders()) {
            String headerName = header.getName();
            //TODO test na velikost pismen
            if (OMITED_HEADERS.contains(headerName)) {
                continue;
            } else if (SET_COOKIE_HEADER.equalsIgnoreCase(headerName) || SET_COOKIE2_HEADER.equalsIgnoreCase(headerName)) {
                rewriteCookieHeader(request, response, header);
            } else {
                response.addHeader(headerName, header.getValue());
            }
        }
    }

    private void rewriteCookieHeader(HttpServletRequest request, HttpServletResponse response, Header header) {

        String newPath = request.getContextPath() + request.getServletPath();

        //parsing by
        // https://tools.ietf.org/html/rfc6265#section-4.1.1
        // https://tools.ietf.org/html/rfc6265#section-5.2

//        http://www.ietf.org/rfc/rfc2109.txt  4.2.2

        String headerValue = header.getValue();
        int version = guessCookieVersion(headerValue);

        List<String> cookies;

        if (version == 0) {
            cookies = new ArrayList<String>(1);
            cookies.add(headerValue);
            // Netscape draft cookie
        } else {
            // rfc2965/2109 cookie
            // if header string contains more than one cookie,
            // it'll separate them with comma
            cookies = splitMultiCookies(headerValue);
        }


        for (int i = 0; i < cookies.size(); i++) {
            cookies.set(i, rewriteOneParsedCookie(cookies.get(i), newPath));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(cookies.get(0));
        for (int i = 1; i < cookies.size(); i++) {
            sb.append(",").append(cookies.get(i));
        }

        response.addHeader(SET_COOKIE_HEADER, sb.toString());

    }

    private String rewriteOneParsedCookie(String setCookieValue, String newPath) {

        int indexOfSemicolon = setCookieValue.indexOf(";");
        String nameValuePair;
        String unparsedAttributes = null;
        if (indexOfSemicolon == -1) {
            nameValuePair = setCookieValue;
        } else {
            nameValuePair = setCookieValue.substring(0, indexOfSemicolon);
            unparsedAttributes = setCookieValue.substring(indexOfSemicolon);//including semicolon
        }

        //TODO set prefix to cookie name

        if (unparsedAttributes == null) {
            return nameValuePair;
        } else {
            unparsedAttributes = rewritePathInCookie(unparsedAttributes, newPath);
            unparsedAttributes = removeDomainFromCookie(unparsedAttributes);
            return nameValuePair + unparsedAttributes;
        }
    }

    private String rewritePathInCookie(String unparsedAttributes, String newPath) {
        String pathString = "path=";
        String unparsedAttributesLowerCase = unparsedAttributes.toLowerCase(Locale.US);
        int indexOfPath = unparsedAttributesLowerCase.indexOf(pathString);
        if (indexOfPath != -1) {
            int endOfPathAttribute = unparsedAttributes.indexOf(";", indexOfPath + pathString.length());
            if (endOfPathAttribute == -1) {
                endOfPathAttribute = unparsedAttributes.length();
            }
            unparsedAttributes = unparsedAttributes.substring(0, indexOfPath + pathString.length())
                    + newPath
                    + unparsedAttributes.substring(endOfPathAttribute);
        }
        return unparsedAttributes;
    }

    private String removeDomainFromCookie(String unparsedAttributes) {
        String domainString = "domain=";
        String unparsedAttributesLowerCase = unparsedAttributes.toLowerCase(Locale.US);
        int indexOfDomain = unparsedAttributesLowerCase.indexOf(domainString);
        if (indexOfDomain != -1) {
            int endOfDomainAttribute = unparsedAttributes.indexOf(';', indexOfDomain + domainString.length());
            if (endOfDomainAttribute == -1) {
                endOfDomainAttribute = unparsedAttributes.length();
            }
            int startOfDomainAttribute = unparsedAttributes.lastIndexOf(';', indexOfDomain);
            if (startOfDomainAttribute == -1) {//only if cookie has invalid format
                startOfDomainAttribute = indexOfDomain;
            }
            unparsedAttributes = unparsedAttributes.substring(0, startOfDomainAttribute)
                    + unparsedAttributes.substring(endOfDomainAttribute);
        }
        return unparsedAttributes;
    }

    /*
     * try to guess the cookie version through set-cookie header string
     * copied from java.net.HttpCookie
     */
    private static int guessCookieVersion(String header) {
        int version = 0;
        header = header.toLowerCase();
        if (header.indexOf("expires=") != -1) {
            // only netscape cookie using 'expires'
            version = 0;
        } else if (header.indexOf("version=") != -1) {
            // version is mandatory for rfc 2965/2109 cookie
            version = 1;
        } else if (header.indexOf("max-age") != -1) {
            // rfc 2965/2109 use 'max-age'
            version = 1;
        }
        return version;
    }

    /*
         * Split cookie header string according to rfc 2965:
         *   1) split where it is a comma;
         *   2) but not the comma surrounding by double-quotes, which is the comma
         *      inside port list or embeded URIs.
         *
         * @param  header
         *         the cookie header string to split
         *
         * @return  list of strings; never null
         */
    private static List<String> splitMultiCookies(String header) {
        List<String> cookies = new java.util.ArrayList<String>();
        int quoteCount = 0;
        int p, q;

        for (p = 0, q = 0; p < header.length(); p++) {
            char c = header.charAt(p);
            if (c == '"') quoteCount++;
            if (c == ',' && (quoteCount % 2 == 0)) {
                // it is comma and not surrounding by double-quotes
                cookies.add(header.substring(q, p));
                q = p + 1;
            }
        }

        cookies.add(header.substring(q));

        return cookies;
    }


    private String getCookieNamePrefix() {
        //TODO implement
        return "!yaps!";
//        return "";
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
