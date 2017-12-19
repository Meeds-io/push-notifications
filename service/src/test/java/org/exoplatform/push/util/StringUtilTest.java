package org.exoplatform.push.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

  @Test
  public void shouldMask() {
    assertEquals("*********", StringUtil.mask("123456789", 0));
    assertEquals("*******89", StringUtil.mask("123456789", 2));
    assertEquals("*****6789", StringUtil.mask("123456789", 4));
    assertEquals("***456789", StringUtil.mask("123456789", 6));
  }

  @Test
  public void shouldNotMask() {
    assertEquals("", StringUtil.mask("", 4));
    assertEquals(null, StringUtil.mask(null, 4));
    assertEquals("123456789", StringUtil.mask("123456789", 20));
  }
}