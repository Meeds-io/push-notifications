package org.exoplatform.push.filter;

import org.exoplatform.web.filter.Filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
