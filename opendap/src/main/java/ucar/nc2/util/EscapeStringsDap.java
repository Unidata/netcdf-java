/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.internal.util;

/** Escape utilities for DAP
 */
public class EscapeStringsDap {
  private static final String _allowableInDAP =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_!~*'-\"./";
  private static final char _URIEscape = '%';

  /**
   * Given a DAP (attribute) string, insert backslashes before '"' and '/' characters. This code
   * also escapes control characters, although the spec does not call for it; make that code
   * conditional.
   */
  public static String backslashEscapeDapString(String s) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (true) {
        if (c < ' ') {
          switch (c) {
            case '\n':
            case '\r':
            case '\t':
            case '\f':
              buf.append((char) c);
              break;
            default:
              buf.append(String.format("\\x%02x", (c & 0xff)));
              break;
          }
          continue;
        }
      }
      if (c == '"') {
        buf.append("\\\"");
      } else if (c == '\\') {
        buf.append("\\\\");
      } else {
        buf.append((char) c);
      }
    }
    return buf.toString();
  }


  /**
   * Given a backslash escaped name, convert to a DAP escaped name
   *
   * @param bs the string to DAP encode; may have backslash escapes
   * @return escaped string
   */

  public static String backslashToDAP(String bs) {
    StringBuilder buf = new StringBuilder();
    int len = bs.length();
    for (int i = 0; i < len; i++) {
      char c = bs.charAt(i);
      if (i < (len - 1) && c == '\\') {
        c = bs.charAt(++i);
      }
      if (_allowableInDAP.indexOf(c) < 0) {
        buf.append(_URIEscape);
        // convert the char to hex
        String ashex = Integer.toHexString(c);
        if (ashex.length() < 2) {
          buf.append('0');
        }
        buf.append(ashex);
      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }


  /**
   * Define the DEFINITIVE opendap identifier unescape function.
   *
   * @param id The identifier to unescape.
   * @return The unescaped identifier.
   */
  public static String unescapeDAPIdentifier(String id) {
    String s;
    try {
      s = EscapeStrings.unescapeString(id);
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  /**
   * Define the DAP escape identifier function.
   *
   * @param s The string to encode.
   * @return The escaped string.
   */
  public static String escapeDAPIdentifier(String s) {
    return EscapeStrings.escapeString(s, _allowableInDAP);
  }

}
