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