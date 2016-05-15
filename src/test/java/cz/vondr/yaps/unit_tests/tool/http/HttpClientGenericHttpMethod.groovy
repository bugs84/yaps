package cz.vondr.yaps.unit_tests.tool.http

import org.apache.http.client.methods.HttpRequestBase

class HttpClientGenericHttpMethod extends HttpRequestBase {

    private String method;

    public HttpClientGenericHttpMethod(URI uri, String method) {
        super();
        setURI(uri);
        this.method = method;
    }

    @Override
    public String getMethod() {
        return method;
    }

}
