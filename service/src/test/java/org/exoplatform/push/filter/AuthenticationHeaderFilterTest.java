package org.exoplatform.push.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AuthenticationHeaderFilterTest {

  @Test
  public void shouldReturnConcatenatedHeaderWithUserAuthenticated() throws IOException, ServletException {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRemoteUser()).thenReturn("john");
    List<Cookie> cookies = new ArrayList<>();
    for (String cookieName : AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE) {
      cookies.add(new Cookie(cookieName, "value" + cookieName));
    }
    when(request.getCookies()).thenReturn(cookies.toArray(new Cookie[0]));

    AuthenticationHeaderFilter filter = new AuthenticationHeaderFilter();

    ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

    String name = headerNameCaptor.getValue();
    String value = headerValueCaptor.getValue();
    assertNotNull(name);
    assertEquals(AuthenticationHeaderFilter.HEADER_AUTHORIZATION, name);
    assertNotNull(value);
    List<String> authenticationHeaderTokens = Arrays.asList(value.split(";"));
    assertEquals(3, authenticationHeaderTokens.size());
    assertTrue(authenticationHeaderTokens.contains(AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[0] + "=value"
        + AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[0]));
    assertTrue(authenticationHeaderTokens.contains(AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[1] + "=value"
        + AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[1]));
    assertTrue(authenticationHeaderTokens.contains(AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[2] + "=value"
        + AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE[2]));
  }

  @Test
  public void shouldReturnConcatenatedHeaderWithNoUserAuthenticated() throws IOException, ServletException {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRemoteUser()).thenReturn(null);
    List<Cookie> cookies = new ArrayList<>();
    for (String cookieName : AuthenticationHeaderFilter.COOKIES_TO_PROPAGATE) {
      cookies.add(new Cookie(cookieName, "value" + cookieName));
    }
    when(request.getCookies()).thenReturn(cookies.toArray(new Cookie[0]));

    AuthenticationHeaderFilter filter = new AuthenticationHeaderFilter();

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response, never()).addHeader(any(), any());
  }

  @Test
  public void shouldReturnConcatenatedHeaderWithUserAuthenticatedAndMissingCookies() throws IOException, ServletException {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRemoteUser()).thenReturn("john");
    List<Cookie> cookies = new ArrayList<>();
    cookies.add(new Cookie("JSESSIONID", "valueJSESSIONID"));
    cookies.add(new Cookie("JSESSIONIDSSO", "valueJSESSIONIDSSO"));
    cookies.add(new Cookie("UNRELATED_COOKIE", "value"));
    when(request.getCookies()).thenReturn(cookies.toArray(new Cookie[0]));

    AuthenticationHeaderFilter filter = new AuthenticationHeaderFilter();

    ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    verify(response).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

    String name = headerNameCaptor.getValue();
    String value = headerValueCaptor.getValue();
    assertNotNull(name);
    assertEquals(AuthenticationHeaderFilter.HEADER_AUTHORIZATION, name);
    assertNotNull(value);
    List<String> authenticationHeaderTokens = Arrays.asList(value.split(";"));
    assertEquals(2, authenticationHeaderTokens.size());
    assertTrue(authenticationHeaderTokens.contains("JSESSIONID=valueJSESSIONID"));
    assertTrue(authenticationHeaderTokens.contains("JSESSIONIDSSO=valueJSESSIONIDSSO"));
  }

  @Test
  public void shouldNotThrowsNPEWhenGettingCookieValueWithNullArray() throws IOException,ServletException {
    try {

      // Given
      HttpServletRequest request = mock(HttpServletRequest.class);
      HttpServletResponse response = mock(HttpServletResponse.class);
      FilterChain filterChain = mock(FilterChain.class);

      when(request.getRemoteUser()).thenReturn("john");
      when(request.getCookies()).thenReturn(null);

      AuthenticationHeaderFilter filter = new AuthenticationHeaderFilter();

      // When
      filter.doFilter(request, response, filterChain);

      // Then
      assertTrue(Boolean.TRUE);
    } catch(NullPointerException ex) {
      fail("NullPointereException in getCookieValue");
    }

  }
}
