/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servlet;

class HttpUtil {

  public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
  public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String DRIVELETTERS = LOWERCASE + UPPERCASE;

  /** Join two string together to form proper path WITHOUT trailing slash */
  public static String canonjoin(String prefix, String suffix) {
    if (prefix == null)
      prefix = "";
    if (suffix == null)
      suffix = "";
    prefix = HttpUtil.canonicalpath(prefix);
    suffix = HttpUtil.canonicalpath(suffix);
    StringBuilder result = new StringBuilder();
    result.append(prefix);
    int prelen = prefix.length();
    if (prelen > 0 && result.charAt(prelen - 1) != '/') {
      result.append('/');
      prelen++;
    }
    if (suffix.length() > 0 && suffix.charAt(0) == '/')
      result.append(suffix.substring(1));
    else
      result.append(suffix);
    int len = result.length();
    if (len > 0 && result.charAt(len - 1) == '/') {
      result.deleteCharAt(len - 1);
      len--;
    }
    return result.toString();
  }

  /**
   * Convert path to use '/' consistently and
   * to remove any trailing '/'
   *
   * @param path convert this path
   * @return canonicalized version
   */
  public static String canonicalpath(String path) {
    if (path == null)
      return null;
    StringBuilder b = new StringBuilder(path);
    canonicalpath(b);
    return b.toString();
  }

  public static void canonicalpath(StringBuilder s) {
    if (s == null || s.length() == 0)
      return;
    int index = 0;
    // "\\" -> "/"
    for (;;) {
      index = s.indexOf("\\", index);
      if (index < 0)
        break;
      s.replace(index, index + 1, "/");
    }
    boolean isabs = (s.charAt(0) == '/'); // remember
    for (;;) { // kill any leading '/'s
      if (s.length() == 0 || s.charAt(0) != '/')
        break;
      s.deleteCharAt(0);
    }
    // Do we have drive letter?
    boolean hasdrive = hasDriveLetter(s);

    if (hasdrive)
      s.setCharAt(0, Character.toLowerCase(s.charAt(0)));

    while (s.length() > 0 && s.charAt(s.length() - 1) == '/') {
      s.deleteCharAt(s.length() - 1); // kill any trailing '/'s
    }

    // Add back leading '/', if any
    if (!hasdrive && isabs)
      s.insert(0, '/');
  }

  /**
   * Convert path to remove any leading '/' or drive letter assumes canonical.
   *
   * @param path convert this path
   * @return relatived version
   */
  public static String relpath(String path) {
    if (path == null)
      return null;
    StringBuilder b = new StringBuilder(path);
    canonicalpath(b);
    if (b.length() > 0) {
      if (b.charAt(0) == '/')
        b.deleteCharAt(0);
      if (hasDriveLetter(b))
        b.delete(0, 2);
    }
    return b.toString();
  }

  /**
   * @param path to test
   * @return true if path appears to start with Windows drive letter
   */
  public static boolean hasDriveLetter(String path) {
    return (path != null && path.length() >= 2 && path.charAt(1) == ':' && DRIVELETTERS.indexOf(path.charAt(0)) >= 0);
  }

  // Support function
  protected static boolean hasDriveLetter(StringBuilder path) {
    return (path.length() >= 2 && path.charAt(1) == ':' && DRIVELETTERS.indexOf(path.charAt(0)) >= 0);
  }

  /**
   * @param path to test
   * @return true if path is absolute
   */
  public static boolean isAbsolutePath(String path) {
    return (path != null && path.length() > 0 && (path.charAt(0) == '/' || hasDriveLetter(path)));
  }

}
