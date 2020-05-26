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

public class StringUtil {

  private final static int MASKED_TEXT_MAX_LENGTH = 15;

  public static String mask(String text, int keep) {
    if(text == null || text.length() == 0) {
      return text;
    }

    String textToMask = text;

    if(textToMask.length() > MASKED_TEXT_MAX_LENGTH) {
      textToMask = text.substring(text.length() - MASKED_TEXT_MAX_LENGTH);
    }

    int maskLength = textToMask.length() - keep;
    if(maskLength < 0) {
      maskLength = 0;
    }

    StringBuilder maskedText = new StringBuilder(textToMask.length());
    for(int i = 0; i < maskLength; i++){
      maskedText.append("x");
    }

    return maskedText.append(textToMask.substring(maskLength)).toString();
  }

}
