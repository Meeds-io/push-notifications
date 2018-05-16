package org.exoplatform.push.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

  @Test
  public void shouldMask() {
    assertEquals("xxxxxxxxxxxxxxx", StringUtil.mask("12345678901234567890", 0));
    assertEquals("xxxxxxxxxxxxx90", StringUtil.mask("12345678901234567890", 2));
    assertEquals("xxxxxxxxxxx7890", StringUtil.mask("12345678901234567890", 4));
    assertEquals("xxxxxxxxx567890", StringUtil.mask("12345678901234567890", 6));
  }

  @Test
  public void shouldNotMask() {
    assertEquals("", StringUtil.mask("", 4));
    assertEquals(null, StringUtil.mask(null, 4));
    assertEquals("123456789", StringUtil.mask("123456789", 20));
  }
}