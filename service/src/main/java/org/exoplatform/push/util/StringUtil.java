package org.exoplatform.push.util;

public class StringUtil {

  public static String mask(String text, int keep) {
    if(text == null || text.length() == 0) {
      return text;
    }

    int maskLength = text.length() - keep;
    if(maskLength < 0) {
      maskLength = 0;
    }

    StringBuilder maskedText = new StringBuilder(text.length());
    for(int i = 0; i < maskLength; i++){
      maskedText.append("*");
    }

    return maskedText.append(text.substring(maskLength)).toString();
  }

}
