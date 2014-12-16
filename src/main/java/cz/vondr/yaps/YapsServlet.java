package cz.vondr.yaps;

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

public class YapsServlet extends HttpServlet {

    protected CloseableHttpClient httpClient;

//    protected String target = "localhost:8080";

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
                .build();


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
    protected void service(HttpServletRequest req, HttpServletResponse servletResponse) throws ServletException, IOException {
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpHost("twitter.com", 80), new BasicHttpEntityEnclosingRequest("GET", "http://twitter.com/jvondrous"));
        try {
            //TODO rewrite 'target response' to 'proxy response'
            servletResponse.setStatus(httpResponse.getStatusLine().getStatusCode());
//            servletResponse.getOutputStream().print("Hello YapsServlet");

            httpResponse.getEntity().writeTo(servletResponse.getOutputStream());


//            servletResponse.getOutputStream()
//            httpResponse.getEntity().getContent()

        } finally {
            httpResponse.close();
            servletResponse.getOutputStream().flush();
            servletResponse.getOutputStream().close();

        }


    }

}
