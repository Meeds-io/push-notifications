/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.push.filter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.*;
import javax.servlet.http.*;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.filter.Filter;

/**
 * Filter setting authentication headers in a custom header. This is required by
 * iOS version older than iOS 11 since they cannot read cookies headers
 * directly.
 */
public class AuthenticationHeaderFilter implements Filter {

  public static final Log             LOG                    = ExoLogger.getLogger(AuthenticationHeaderFilter.class);

  public static final String          HEADER_AUTHORIZATION   = "X-Authorization";

  public static final String          REMEMBER_ME_COOKIE     = "rememberme";

  public static final String          JSESSION_ID_SSO_COOKIE = "JSESSIONIDSSO";

  public static final String          JSESSION_ID_COOKIE     = "JSESSIONID";

  protected static final List<String> COOKIES_TO_PROPAGATE   = Arrays.asList(JSESSION_ID_COOKIE,
                                                                             JSESSION_ID_SSO_COOKIE,
                                                                             REMEMBER_ME_COOKIE);

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
                                                                                                  ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    try {
      Cookie[] cookies = req.getCookies();
      if (req.getRemoteUser() != null && cookies != null && cookies.length > 0) {
        Map<String, String> authorizationHeaders = new HashMap<>();
        for (String cookieName : COOKIES_TO_PROPAGATE) {
          String cookieValue = getCookieValue(cookies, cookieName);
          if (cookieValue != null) {
            authorizationHeaders.put(cookieName, cookieValue);
          }
        }

        if (!authorizationHeaders.isEmpty()) {
          String authorizationHeaderValue = authorizationHeaders.keySet()
                                                                .stream()
                                                                .map(key -> key + "=" + authorizationHeaders.get(key))
                                                                .collect(Collectors.joining(";"));
          res.addHeader(HEADER_AUTHORIZATION, authorizationHeaderValue);
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while computing {} Header for user {}",
               HEADER_AUTHORIZATION,
               req.getRemoteUser(),
               e);
    } finally {
      filterChain.doFilter(req, res);
    }
  }

  protected String getCookieValue(Cookie[] cookies, Object key) {
    if (cookies != null) {
      for (int i = 0; i < cookies.length; i++) {
        if (key.equals(cookies[i].getName())) {
          return cookies[i].getValue();
        }
      }
    }
    return null;
  }
}
