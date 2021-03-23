/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import thredds.inventory.MFile;

/**
 * A wildcard expression that matches on the MFile name.
 *
 * @author caron
 * @since Jun 26, 2009
 */
public class WildcardMatchOnName extends WildcardMatchOnPath {
  public WildcardMatchOnName(String wildcardString) {
    super(wildcardString);
  }

  public WildcardMatchOnName(Pattern pattern) {
    super(pattern);
  }

  public boolean accept(MFile file) {
    Matcher matcher = this.pattern.matcher(file.getName());
    return matcher.matches();
  }
}
