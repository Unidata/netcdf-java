/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.filter;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import thredds.inventory.MFileFilter;
import thredds.inventory.MFile;

/**
 * A java.nio.file.DirectoryStream.Filter using a regexp on the last entry of the MFile path
 */
public class RegExpMatch implements MFileFilter {
  private final Pattern pattern;
  private final boolean nameOnly;

  public RegExpMatch(Pattern pattern, boolean nameOnly) {
    this.pattern = pattern;
    this.nameOnly = nameOnly;
  }

  @Override
  public boolean accept(MFile mfile) {
    String matchOn = nameOnly ? mfile.getName() : mfile.getPath().replace('\\', '/');
    Matcher matcher = this.pattern.matcher(matchOn);
    return matcher.matches();
  }
}
