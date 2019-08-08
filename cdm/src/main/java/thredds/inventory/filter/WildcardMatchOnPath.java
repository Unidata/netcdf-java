/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.filter;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import thredds.inventory.MFileFilter;
import thredds.inventory.MFile;

/**
 * A wildcard expression that matches on the MFile path.
 *
 * @author caron
 * @since Jun 26, 2009
 */
public class WildcardMatchOnPath implements MFileFilter {
  private static final boolean debug = false;
  private String wildcardString;
  protected Pattern pattern;

  public WildcardMatchOnPath(String wildcardString) {
    this.wildcardString = wildcardString;

    String regExp = wildcardString.replaceAll("\\.", "\\\\."); // Replace "." with "\.".
    regExp = regExp.replaceAll("\\*", ".*"); // Replace "*" with ".*".
    regExp = regExp.replaceAll("\\?", ".?"); // Replace "?" with ".?".

    // Compile regular expression pattern
    this.pattern = Pattern.compile(regExp);
  }

  WildcardMatchOnPath(Pattern pattern) {
    this.pattern = pattern;
  }

  public boolean accept(MFile file) {
    String p = file.getPath();
    Matcher matcher = this.pattern.matcher(p);
    if (debug)
      System.out.printf(" WildcardMatchOnPath regexp %s on %s match=%s%n", pattern, file.getPath(), matcher.matches());
    return matcher.matches();
  }

  @Override
  public String toString() {
    return "WildcardMatchOnPath{" +
            "wildcard=" + wildcardString +
            " regexp=" + pattern +
            '}';
  }
}