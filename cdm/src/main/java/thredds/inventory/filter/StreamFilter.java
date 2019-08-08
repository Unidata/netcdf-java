/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;

/**
 * A java.nio.file.DirectoryStream.Filter using a regexp on the last entry of the Path
 *
 * @author John
 * @since 1/28/14
 */
public class StreamFilter implements DirectoryStream.Filter<Path> {
  private Pattern pattern;
  private boolean nameOnly;

  public StreamFilter(Pattern pattern, boolean nameOnly) {
    this.pattern = pattern;
    this.nameOnly = nameOnly;
  }

  @Override
  public boolean accept(Path entry) throws IOException {
    String matchOn = nameOnly ? entry.getName(entry.getNameCount()-1).toString() : StringUtil2.replace(entry.toString(), "\\", "/");
    Matcher matcher = this.pattern.matcher(matchOn);
    return matcher.matches();
  }
}
