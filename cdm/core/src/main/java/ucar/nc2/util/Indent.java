/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

/** Maintains indentation level for printing nested structures. */
public class Indent {
  private final int nspaces;

  private int level;
  private final StringBuilder blanks = new StringBuilder();
  private String indent = "";

  /** Create an Indent with nspaces per level. */
  public Indent(int nspaces) {
    this.nspaces = nspaces;
    makeBlanks(100);
  }

  /** Increment the indent level */
  public Indent incr() {
    level++;
    setIndentLevel(level);
    return this;
  }

  /** Decrement the indent level */
  public Indent decr() {
    level--;
    setIndentLevel(level);
    return this;
  }

  /** Get the indent level */
  public int level() {
    return level;
  }

  /** Return a String of nspaces * level blanks which is the indentation. */
  public String toString() {
    return indent;
  }

  /** Set the indent level */
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
