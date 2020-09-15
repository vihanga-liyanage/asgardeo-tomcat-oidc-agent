/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.asgardio.tomcat.oidc.agent;

import io.asgardio.java.oidc.sdk.OIDCConfigProvider;
import io.asgardio.java.oidc.sdk.OIDCManager;
import io.asgardio.java.oidc.sdk.OIDCManagerImpl;
import io.asgardio.java.oidc.sdk.OIDCRequestResolver;
import io.asgardio.java.oidc.sdk.bean.OIDCAgentConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OIDCAgentFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(OIDCAgentFilter.class);

    protected FilterConfig filterConfig = null;
    OIDCAgentConfig oidcAgentConfig;
    OIDCManager oidcManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        this.filterConfig = filterConfig;
        OIDCConfigProvider oidcConfigProvider = new OIDCConfigProvider(filterConfig.getServletContext());
        oidcConfigProvider.init();
        this.oidcAgentConfig = oidcConfigProvider.getOidcAgentConfig();
        this.oidcManager = new OIDCManagerImpl(oidcAgentConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        OIDCRequestResolver requestResolver = new OIDCRequestResolver(request, oidcAgentConfig);

        if (requestResolver.isSkipURI()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (requestResolver.isLogoutURL()) {
            oidcManager.singleLogout(request, response);
            return;
        }

        if (requestResolver.isCallbackResponse()) {
            oidcManager.handleOIDCCallback(request, response);
            return;
        }

        if (!oidcManager.isActiveSessionPresent(request)) {
            oidcManager.login(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}
