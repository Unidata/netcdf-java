/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

/** Maintains indentation level for printing nested structures. */
public class Indent {
  private int nspaces;

  private int level;
  private final StringBuilder blanks = new StringBuilder();
  private String indent = "";

  // nspaces = how many spaces each level adds.
  // max 100 levels
  public Indent(int nspaces) {
    this.nspaces = nspaces;
    makeBlanks(100);
  }

  public Indent incr() {
    level++;
    setIndentLevel(level);
    return this;
  }

  public Indent decr() {
    level--;
    setIndentLevel(level);
    return this;
  }

  public int level() {
    return level;
  }

  public String toString() {
    return indent;
  }

  public void setIndentLevel(int level) {
    this.level = level;
    if (level * nspaces >= blanks.length())
      makeBlanks(100);
    int end = Math.min(level * nspaces, blanks.length());
    indent = blanks.substring(0, end);
  }

  private void makeBlanks(int len) {
    for (int i = 0; i < len * nspaces; i++)
      blanks.append(" ");
  }
}
