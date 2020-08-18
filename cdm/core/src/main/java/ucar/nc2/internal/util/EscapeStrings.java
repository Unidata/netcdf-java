/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import ucar.nc2.constants.CDM;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Text Escaping utilities.
 * TODO replace with standard escape libraries where possible
 */
public class EscapeStrings {
  static final String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  static final String _allowableInOGC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()";
  private static final String _allowableInUrlQuery =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'()*+,-./:;=?@_~";

  private static final char _URIEscape = '%';
  private static final byte blank = ((byte) ' ');
  private static final byte plus = ((byte) '+');

  private static final byte hexa = (byte) 'a';
  private static final byte hexf = (byte) 'f';
  private static final byte hexA = (byte) 'A';
  private static final byte hexF = (byte) 'F';
  private static final byte hex0 = (byte) '0';
  private static final byte hex9 = (byte) '9';
  private static final byte ten = (byte) 10;
  private static final int sep = '.';

  ///////////////////////////////////////////////////////////////

  /**
   * backslash escape a string
   *
   * @param x escape this; may be null
   * @param reservedChars these chars get a backslash in front of them
   * @return escaped string
   */
  public static String backslashEscape(String x, String reservedChars) {
    if (x == null) {
      return null;
    } else if (reservedChars == null) {
      return x;
    }

    boolean ok = true;
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (reservedChars.indexOf(c) >= 0) {
        ok = false;
        break;
      }
    }
    if (ok) {
      return x;
    }

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (reservedChars.indexOf(c) < 0) {
        continue;
      }

      sb.setCharAt(pos, '\\');
      pos++;
      sb.insert(pos, c);
      pos++;
    }

    return sb.toString();
  }

  /**
   * backslash unescape a string
   *
   * @param x unescape this
   * @return string with \c -> c
   */
  public static String backslashUnescape(String x) {
    if (!x.contains("\\")) {
      return x;
    }

    // gotta do it
    StringBuilder sb = new StringBuilder(x.length());
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (c == '\\') {
        c = x.charAt(++pos); // skip backslash, get next cha
      }
      sb.append(c);
    }

    return sb.toString();
  }

  /**
   * Tokenize an escaped name using "." as delimiter, skipping "\."
   *
   * @param escapedName an escaped name
   * @return list of tokens
   */
  public static List<String> tokenizeEscapedName(String escapedName) {
    List<String> result = new ArrayList<>();
    int pos = 0;
    int start = 0;
    while (true) {
      pos = escapedName.indexOf(sep, pos + 1);
      if (pos <= 0) {
        break;
      }
      if (escapedName.charAt(pos - 1) != '\\') {
        result.add(escapedName.substring(start, pos));
        start = pos + 1;
      }
    }
    result.add(escapedName.substring(start)); // remaining
    return result;
  }

  /** Given a CDM string, insert backslashes before <toescape> characters. */
  public static String backslashEscapeCDMString(String s, String toescape) {
    if (toescape == null || toescape.isEmpty()) {
      return s;
    }
    if (s == null || s.isEmpty()) {
      return s;
    }
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (toescape.indexOf(c) >= 0) {
        buf.append('\\');
      }
      buf.append((char) c);
    }
    return buf.toString();
  }

  /**
   * Find first occurence of char c in escapedName, excluding escaped c.
   *
   * @param escapedName search in this string
   * @param c for this char but not \\cha
   * @return pos in string, or -1
   */
  public static int indexOf(String escapedName, char c) {
    int pos = 0;
    while (true) {
      pos = escapedName.indexOf(c, pos + 1);
      if (pos <= 0) {
        return pos;
      }
      if (escapedName.charAt(pos - 1) != '\\') {
        return pos;
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * The OGC Web Services escape function.
   *
   * @param s The string to encode.
   * @return The escaped string, or null on error.
   */
  @Nullable
  public static String escapeOGC(String s) {
    return escapeString(s, _allowableInOGC);
  }

  /**
   * Define the DEFINITIVE URL constraint expression escape function.
   *
   * @param ce The expression to modify.
   * @return The escaped string, or null on error.
   */
  @Nullable
  public static String escapeURLQuery(String ce) {
    return escapeString(ce, _allowableInUrlQuery);
  }

  /** Public by accident, do not use directly */
  @Nullable
  public static String escapeString(String in, String allowable) {
    return xescapeString(in, allowable, _URIEscape, false);
  }

  /**
   * @param in String to escape
   * @param allowable allowedcharacters
   * @param esc escape char prefix
   * @param spaceplus true =>convert ' ' to '+'
   */
  @Nullable
  private static String xescapeString(String in, String allowable, char esc, boolean spaceplus) {
    try {
      StringBuilder out = new StringBuilder();
      if (in == null) {
        return null;
      }
      byte[] utf8 = in.getBytes(StandardCharsets.UTF_8);
      byte[] allow8 = allowable.getBytes(StandardCharsets.UTF_8);
      for (byte b : utf8) {
        if (b == blank && spaceplus) {
          out.append('+');
        } else {
          // search allow8
          boolean found = false;
          for (byte a : allow8) {
            if (a == b) {
              found = true;
              break;
            }
          }
          if (found) {
            out.append((char) b);
          } else {
            String c = Integer.toHexString(b);
            out.append(esc);
            if (c.length() < 2) {
              out.append('0');
            }
            out.append(c);
          }
        }
      }

      return out.toString();

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Define the DEFINITIVE URL constraint expression unescape function.
   *
   * @param ce The expression to unescape.
   * @return The unescaped expression, or null on error.
   */
  @Nullable
  public static String unescapeURLQuery(String ce) {
    return unescapeString(ce);
  }

  /** Public by accident, do not use directly */
  @Nullable
  public static String unescapeString(String in) {
    return xunescapeString(in, _URIEscape, false);
  }

  /**
   * Given a string that contains WWW escape sequences, translate those escape sequences back into
   * ASCII characters. Return the modified string.
   *
   * @param in The string to modify.
   * @param escape The character used to signal the begining of an escape sequence. param except If
   *        there is some escape code that should not be removed by this call (e.g., you might not want to
   *        remove spaces, %20) use this parameter to specify that code. The function will then transform
   *        all escapes except that one.
   * @param spaceplus True if spaces should be replaced by '+'.
   * @return The modified string.
   */
  @Nullable
  private static String xunescapeString(String in, char escape, boolean spaceplus) {
    try {
      if (in == null) {
        return null;
      }

      byte[] utf8 = in.getBytes(StandardCharsets.UTF_8);
      byte escape8 = (byte) escape;
      byte[] out = new byte[utf8.length]; // Should be max we need

      int index8 = 0;
      for (int i = 0; i < utf8.length;) {
        byte b = utf8[i++];
        if (b == plus && spaceplus) {
          out[index8++] = blank;
        } else if (b == escape8) {
          // check to see if there are enough characters left
          if (i + 2 <= utf8.length) {
            b = (byte) (fromHex(utf8[i]) << 4 | fromHex(utf8[i + 1]));
            i += 2;
          }
        }
        out[index8++] = b;
      }
      return new String(out, 0, index8, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return null;
    }

  }

  private static byte fromHex(byte b) throws NumberFormatException {
    if (b >= hex0 && b <= hex9) {
      return (byte) (b - hex0);
    }
    if (b >= hexa && b <= hexf) {
      return (byte) (ten + (b - hexa));
    }
    if (b >= hexA && b <= hexF) {
      return (byte) (ten + (b - hexA));
    }
    throw new NumberFormatException("Illegal hex character: " + b);
  }

  /**
   * A cover for URLDecoder.decode(): Decodes a {@code application/x-www-form-urlencoded} string.
   * Returns null instead of throwing an Exception.
   * 
   * @param s The string to unescape.
   * @return The unescaped expression, or null on error
   */
  @Nullable
  public static String urlDecode(String s) {
    try {
      s = URLDecoder.decode(s, CDM.UTF8);
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

}
