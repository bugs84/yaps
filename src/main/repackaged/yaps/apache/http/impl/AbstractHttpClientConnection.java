/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package yaps.apache.http.impl;

import yaps.apache.http.HttpClientConnection;
import yaps.apache.http.HttpConnectionMetrics;
import yaps.apache.http.HttpEntity;
import yaps.apache.http.HttpEntityEnclosingRequest;
import yaps.apache.http.HttpException;
import yaps.apache.http.HttpRequest;
import yaps.apache.http.HttpResponse;
import yaps.apache.http.HttpResponseFactory;
import yaps.apache.http.HttpStatus;
import yaps.apache.http.annotation.NotThreadSafe;
import yaps.apache.http.impl.entity.EntityDeserializer;
import yaps.apache.http.impl.entity.EntitySerializer;
import yaps.apache.http.impl.entity.LaxContentLengthStrategy;
import yaps.apache.http.impl.entity.StrictContentLengthStrategy;
import yaps.apache.http.impl.io.DefaultHttpResponseParser;
import yaps.apache.http.impl.io.HttpRequestWriter;
import yaps.apache.http.io.EofSensor;
import yaps.apache.http.io.HttpMessageParser;
import yaps.apache.http.io.HttpMessageWriter;
import yaps.apache.http.io.HttpTransportMetrics;
import yaps.apache.http.io.SessionInputBuffer;
import yaps.apache.http.io.SessionOutputBuffer;
import yaps.apache.http.params.HttpParams;
import yaps.apache.http.util.Args;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Abstract client-side HTTP connection capable of transmitting and receiving
 * data using arbitrary {@link SessionInputBuffer} and
 * {@link SessionOutputBuffer} implementations.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link yaps.apache.http.params.CoreProtocolPNames#STRICT_TRANSFER_ENCODING}</li>
 *  <li>{@link yaps.apache.http.params.CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link yaps.apache.http.params.CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 * </ul>
 *
 * @since 4.0
 *
 * @deprecated (4.3) use {@link DefaultBHttpClientConnection}
 */
@NotThreadSafe
@Deprecated
public abstract class AbstractHttpClientConnection implements HttpClientConnection {

    private final EntitySerializer entityserializer;
    private final EntityDeserializer entitydeserializer;

    private SessionInputBuffer inbuffer = null;
    private SessionOutputBuffer outbuffer = null;
    private EofSensor eofSensor = null;
    private HttpMessageParser<HttpResponse> responseParser = null;
    private HttpMessageWriter<HttpRequest> requestWriter = null;
    private HttpConnectionMetricsImpl metrics = null;

    /**
     * Creates an instance of this class.
     * <p>
     * This constructor will invoke {@link #createEntityDeserializer()}
     * and {@link #createEntitySerializer()} methods in order to initialize
     * HTTP entity serializer and deserializer implementations for this
     * connection.
     */
    public AbstractHttpClientConnection() {
        super();
        this.entityserializer = createEntitySerializer();
        this.entitydeserializer = createEntityDeserializer();
    }

    /**
     * Asserts if the connection is open.
     *
     * @throws IllegalStateException if the connection is not open.
     */
    protected abstract void assertOpen() throws IllegalStateException;

    /**
     * Creates an instance of {@link EntityDeserializer} with the
     * {@link LaxContentLengthStrategy} implementation to be used for
     * de-serializing entities received over this connection.
     * <p>
     * This method can be overridden in a super class in order to create
     * instances of {@link EntityDeserializer} using a custom
     * {@link yaps.apache.http.entity.ContentLengthStrategy}.
     *
     * @return HTTP entity deserializer
     */
    protected EntityDeserializer createEntityDeserializer() {
        return new EntityDeserializer(new LaxContentLengthStrategy());
    }

    /**
     * Creates an instance of {@link EntitySerializer} with the
     * {@link StrictContentLengthStrategy} implementation to be used for
     * serializing HTTP entities sent over this connection.
     * <p>
     * This method can be overridden in a super class in order to create
     * instances of {@link EntitySerializer} using a custom
     * {@link yaps.apache.http.entity.ContentLengthStrategy}.
     *
     * @return HTTP entity serialzier.
     */
    protected EntitySerializer createEntitySerializer() {
        return new EntitySerializer(new StrictContentLengthStrategy());
    }

    /**
     * Creates an instance of {@link DefaultHttpResponseFactory} to be used
     * for creating {@link HttpResponse} objects received by over this
     * connection.
     * <p>
     * This method can be overridden in a super class in order to provide
     * a different implementation of the {@link HttpResponseFactory} interface.
     *
     * @return HTTP response factory.
     */
    protected HttpResponseFactory createHttpResponseFactory() {
        return DefaultHttpResponseFactory.INSTANCE;
    }

    /**
     * Creates an instance of {@link HttpMessageParser} to be used for parsing
     * HTTP responses received over this connection.
     * <p>
     * This method can be overridden in a super class in order to provide
     * a different implementation of the {@link HttpMessageParser} interface or
     * to pass a different implementation of the
     * {@link yaps.apache.http.message.LineParser} to the the
     * {@link DefaultHttpResponseParser} constructor.
     *
     * @param buffer the session input buffer.
     * @param responseFactory the HTTP response factory.
     * @param params HTTP parameters.
     * @return HTTP message parser.
     */
    protected HttpMessageParser<HttpResponse> createResponseParser(
            final SessionInputBuffer buffer,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        return new DefaultHttpResponseParser(buffer, null, responseFactory, params);
    }

    /**
     * Creates an instance of {@link HttpMessageWriter} to be used for
     * writing out HTTP requests sent over this connection.
     * <p>
     * This method can be overridden in a super class in order to provide
     * a different implementation of the {@link HttpMessageWriter} interface or
     * to pass a different implementation of
     * {@link yaps.apache.http.message.LineFormatter} to the the default implementation
     * {@link HttpRequestWriter}.
     *
     * @param buffer the session output buffer
     * @param params HTTP parameters
     * @return HTTP message writer
     */
    protected HttpMessageWriter<HttpRequest> createRequestWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        return new HttpRequestWriter(buffer, null, params);
    }

    /**
     * @since 4.1
     */
    protected HttpConnectionMetricsImpl createConnectionMetrics(
            final HttpTransportMetrics inTransportMetric,
            final HttpTransportMetrics outTransportMetric) {
        return new HttpConnectionMetricsImpl(inTransportMetric, outTransportMetric);
    }

    /**
     * Initializes this connection object with {@link SessionInputBuffer} and
     * {@link SessionOutputBuffer} instances to be used for sending and
     * receiving data. These session buffers can be bound to any arbitrary
     * physical output medium.
     * <p>
     * This method will invoke {@link #createHttpResponseFactory()},
     * {@link #createRequestWriter(SessionOutputBuffer, HttpParams)}
     * and {@link #createResponseParser(SessionInputBuffer, HttpResponseFactory, HttpParams)}
     * methods to initialize HTTP request writer and response parser for this
     * connection.
     *
     * @param inbuffer the session input buffer.
     * @param outbuffer the session output buffer.
     * @param params HTTP parameters.
     */
    protected void init(
            final SessionInputBuffer inbuffer,
            final SessionOutputBuffer outbuffer,
            final HttpParams params) {
        this.inbuffer = Args.notNull(inbuffer, "Input session buffer");
        this.outbuffer = Args.notNull(outbuffer, "Output session buffer");
        if (inbuffer instanceof EofSensor) {
            this.eofSensor = (EofSensor) inbuffer;
        }
        this.responseParser = createResponseParser(
                inbuffer,
                createHttpResponseFactory(),
                params);
        this.requestWriter = createRequestWriter(
                outbuffer, params);
        this.metrics = createConnectionMetrics(
                inbuffer.getMetrics(),
                outbuffer.getMetrics());
    }

    public boolean isResponseAvailable(final int timeout) throws IOException {
        assertOpen();
        try {
            return this.inbuffer.isDataAvailable(timeout);
        } catch (final SocketTimeoutException ex) {
            return false;
        }
    }

    public void sendRequestHeader(final HttpRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        assertOpen();
        this.requestWriter.write(request);
        this.metrics.incrementRequestCount();
    }

    public void sendRequestEntity(final HttpEntityEnclosingRequest request)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        assertOpen();
        if (request.getEntity() == null) {
            return;
        }
        this.entityserializer.serialize(
                this.outbuffer,
                request,
                request.getEntity());
    }

    protected void doFlush() throws IOException {
        this.outbuffer.flush();
    }

    public void flush() throws IOException {
        assertOpen();
        doFlush();
    }

    public HttpResponse receiveResponseHeader()
            throws HttpException, IOException {
        assertOpen();
        final HttpResponse response = this.responseParser.parse();
        if (response.getStatusLine().getStatusCode() >= HttpStatus.SC_OK) {
            this.metrics.incrementResponseCount();
        }
        return response;
    }

    public void receiveResponseEntity(final HttpResponse response)
            throws HttpException, IOException {
        Args.notNull(response, "HTTP response");
        assertOpen();
        final HttpEntity entity = this.entitydeserializer.deserialize(this.inbuffer, response);
        response.setEntity(entity);
    }

    protected boolean isEof() {
        return this.eofSensor != null && this.eofSensor.isEof();
    }

    public boolean isStale() {
        if (!isOpen()) {
            return true;
        }
        if (isEof()) {
            return true;
        }
        try {
            this.inbuffer.isDataAvailable(1);
            return isEof();
        } catch (final SocketTimeoutException ex) {
            return false;
        } catch (final IOException ex) {
            return true;
        }
    }

    public HttpConnectionMetrics getMetrics() {
        return this.metrics;
    }

}
