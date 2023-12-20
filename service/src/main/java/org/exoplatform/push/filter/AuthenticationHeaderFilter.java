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

import org.exoplatform.web.filter.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter setting authentication headers in a custom header. This is required by
 * iOS version older than iOS 11 since they cannot read cookies headers directly.
 */
public class AuthenticationHeaderFilter implements Filter {

  public static final String   HEADER_AUTHORIZATION = "X-Authorization";

  public static final String[] COOKIES_TO_PROPAGATE = new String[] { "JSESSIONID", "JSESSIONIDSSO", "rememberme" };

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
                                                                                                  ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (req.getRemoteUser() != null) {
      Map<String, String> authorizationHeaders = new HashMap<>();
      Cookie[] cookies = req.getCookies();

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

    filterChain.doFilter(req, res);
    return;
  }

  protected String getCookieValue(Cookie[] cookies, Object key) {
    if (cookies!=null) {
      for (int i = 0; i < cookies.length; i++) {
        if (key.equals(cookies[i].getName())) {
          return cookies[i].getValue();
        }
      }
    }
    return null;
  }
}
