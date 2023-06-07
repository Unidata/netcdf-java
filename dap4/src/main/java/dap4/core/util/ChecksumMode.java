/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.util;

/**
 * Define possible checksum modes:
 *
 */
public enum ChecksumMode {
  NONE, // => dap4.checksum was not specified
  FALSE, // => dap4.checksum=false
  TRUE; // => dap4.checksum=true

  static public final ChecksumMode dfalt = TRUE; // Must be TRUE|FALSE

  static final String[] trues = new String[] {"true", "on", "yes", "1"};
  static final String[] falses = new String[] {"false", "off", "no", "0"};

  static public String toString(ChecksumMode mode) {
    if (mode == null)
      mode = ChecksumMode.dfalt;
    switch (mode) {
      case NONE:
        return toString(ChecksumMode.FALSE);
      case FALSE:
        return "false";
      case TRUE:
        return "true";
    }
    return ChecksumMode.toString(dfalt);
  }

  static public ChecksumMode modeFor(String s) {
    if (s == null || s.length() == 0)
      return null;
    for (String f : falses) {
      if (f.equalsIgnoreCase(s))
        return FALSE;
    }
    for (String t : trues) {
      if (t.equalsIgnoreCase(s))
        return TRUE;
    }
    return null;
  }

  /**
   * force mode to be TRUE or FALSE
   * 
   * @param mode
   * @return TRUE|FALSE
   */
  static public ChecksumMode asTrueFalse(ChecksumMode mode) {
    if (mode == null || mode == NONE)
      mode = dfalt;
    return mode;
  }

}
