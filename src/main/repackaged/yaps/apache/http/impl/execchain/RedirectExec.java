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

package yaps.apache.http.impl.execchain;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import yaps.apache.http.HttpEntityEnclosingRequest;
import yaps.apache.http.HttpException;
import yaps.apache.http.HttpHost;
import yaps.apache.http.HttpRequest;
import yaps.apache.http.ProtocolException;
import yaps.apache.http.annotation.ThreadSafe;
import yaps.apache.http.auth.AuthScheme;
import yaps.apache.http.auth.AuthState;
import yaps.apache.http.client.RedirectException;
import yaps.apache.http.client.RedirectStrategy;
import yaps.apache.http.client.config.RequestConfig;
import yaps.apache.http.client.methods.CloseableHttpResponse;
import yaps.apache.http.client.methods.HttpExecutionAware;
import yaps.apache.http.client.methods.HttpRequestWrapper;
import yaps.apache.http.client.protocol.HttpClientContext;
import yaps.apache.http.client.utils.URIUtils;
import yaps.apache.http.conn.routing.HttpRoute;
import yaps.apache.http.conn.routing.HttpRoutePlanner;
import yaps.apache.http.util.Args;
import yaps.apache.http.util.EntityUtils;

/**
 * Request executor in the request execution chain that is responsible
 * for handling of request redirects.
 * <p>
 * Further responsibilities such as communication with the opposite
 * endpoint is delegated to the next executor in the request execution
 * chain.
 * </p>
 *
 * @since 4.3
 */
@ThreadSafe
public class RedirectExec implements ClientExecChain {

    private final Log log = LogFactory.getLog(getClass());

    private final ClientExecChain requestExecutor;
    private final RedirectStrategy redirectStrategy;
    private final HttpRoutePlanner routePlanner;

    public RedirectExec(
            final ClientExecChain requestExecutor,
            final HttpRoutePlanner routePlanner,
            final RedirectStrategy redirectStrategy) {
        super();
        Args.notNull(requestExecutor, "HTTP client request executor");
        Args.notNull(routePlanner, "HTTP route planner");
        Args.notNull(redirectStrategy, "HTTP redirect strategy");
        this.requestExecutor = requestExecutor;
        this.routePlanner = routePlanner;
        this.redirectStrategy = redirectStrategy;
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpRoute route,
            final HttpRequestWrapper request,
            final HttpClientContext context,
            final HttpExecutionAware execAware) throws IOException, HttpException {
        Args.notNull(route, "HTTP route");
        Args.notNull(request, "HTTP request");
        Args.notNull(context, "HTTP context");

        final List<URI> redirectLocations = context.getRedirectLocations();
        if (redirectLocations != null) {
            redirectLocations.clear();
        }

        final RequestConfig config = context.getRequestConfig();
        final int maxRedirects = config.getMaxRedirects() > 0 ? config.getMaxRedirects() : 50;
        HttpRoute currentRoute = route;
        HttpRequestWrapper currentRequest = request;
        for (int redirectCount = 0;;) {
            final CloseableHttpResponse response = requestExecutor.execute(
                    currentRoute, currentRequest, context, execAware);
            try {
                if (config.isRedirectsEnabled() &&
                        this.redirectStrategy.isRedirected(currentRequest, response, context)) {

                    if (redirectCount >= maxRedirects) {
                        throw new RedirectException("Maximum redirects ("+ maxRedirects + ") exceeded");
                    }
                    redirectCount++;

                    final HttpRequest redirect = this.redirectStrategy.getRedirect(
                            currentRequest, response, context);
                    if (!redirect.headerIterator().hasNext()) {
                        final HttpRequest original = request.getOriginal();
                        redirect.setHeaders(original.getAllHeaders());
                    }
                    currentRequest = HttpRequestWrapper.wrap(redirect);

                    if (currentRequest instanceof HttpEntityEnclosingRequest) {
                        RequestEntityProxy.enhance((HttpEntityEnclosingRequest) currentRequest);
                    }

                    final URI uri = currentRequest.getURI();
                    final HttpHost newTarget = URIUtils.extractHost(uri);
                    if (newTarget == null) {
                        throw new ProtocolException("Redirect URI does not specify a valid host name: " +
                                uri);
                    }

                    // Reset virtual host and auth states if redirecting to another host
                    if (!currentRoute.getTargetHost().equals(newTarget)) {
                        final AuthState targetAuthState = context.getTargetAuthState();
                        if (targetAuthState != null) {
                            this.log.debug("Resetting target auth state");
                            targetAuthState.reset();
                        }
                        final AuthState proxyAuthState = context.getProxyAuthState();
                        if (proxyAuthState != null) {
                            final AuthScheme authScheme = proxyAuthState.getAuthScheme();
                            if (authScheme != null && authScheme.isConnectionBased()) {
                                this.log.debug("Resetting proxy auth state");
                                proxyAuthState.reset();
                            }
                        }
                    }

                    currentRoute = this.routePlanner.determineRoute(newTarget, currentRequest, context);
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Redirecting to '" + uri + "' via " + currentRoute);
                    }
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } else {
                    return response;
                }
            } catch (final RuntimeException ex) {
                response.close();
                throw ex;
            } catch (final IOException ex) {
                response.close();
                throw ex;
            } catch (final HttpException ex) {
                // Protocol exception related to a direct.
                // The underlying connection may still be salvaged.
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (final IOException ioex) {
                    this.log.debug("I/O error while releasing connection", ioex);
                } finally {
                    response.close();
                }
                throw ex;
            }
        }
    }

}
