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
      maskedText.append("*");
    }

    return maskedText.append(textToMask.substring(maskLength)).toString();
  }

}
