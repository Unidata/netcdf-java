/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ucar.unidata.util.StringUtil2;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Formatter;

/**
 * Abstract base class for parsing collection specification strings
 */
@ThreadSafe
public abstract class CollectionSpecParserAbstract {
  protected final String spec;
  protected final String rootDir;
  protected final boolean subdirs; // recurse into subdirectories under the root dir
  protected final boolean filterOnName; // filter on name, else on entire path
  protected final Pattern filter; // regexp filter
  protected final String dateFormatMark;
  protected final String delimiter;
  protected final String fragment;

  /**
   * Single spec : "/topdir/** /#dateFormatMark#regExp"
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
   * and you can't use any part of the dateFormat to filter on.
   *
   * @param collectionSpec the collection spec
   * @param rootDir the root directory
   * @param filterAndDateMark the part of spec containing filter and/ or date mark
   * @param delimiter the delimiter in the file path
   * @param fragment in path
   * @param errlog put error messages here, may be null
   */
  protected CollectionSpecParserAbstract(String collectionSpec, String rootDir, String filterAndDateMark,
      String delimiter, String fragment, Formatter errlog) {
    this.spec = collectionSpec.trim();

    this.rootDir = rootDir;
    this.subdirs = collectionSpec.contains(delimiter + "**" + delimiter);
    this.filter = getRegEx(filterAndDateMark);
    this.dateFormatMark = getDateFormatMark(filterAndDateMark);
    this.delimiter = delimiter;
    this.fragment = fragment;
    this.filterOnName = true;
  }

  /**
   * @param rootDir the root directory
   * @param regExp the regular expression to use as a filter
   * @param delimiter the delimiter in the file path
   * @param fragment in path
   * @param errlog put error messages here, may be null
   */
  protected CollectionSpecParserAbstract(String rootDir, String regExp, String delimiter, String fragment,
      Formatter errlog) {
    this.rootDir = StringUtil2.removeFromEnd(rootDir, delimiter);
    this.subdirs = true;
    this.spec = this.rootDir + delimiter + regExp;
    this.filter = Pattern.compile(spec);
    this.dateFormatMark = null;
    this.delimiter = delimiter;
    this.fragment = fragment;
    this.filterOnName = false;
  }

  protected static String getRootDir(String collectionSpec, String defaultRootDir, String delimiter) {
    final String rootDir = splitOnLastDelimiter(collectionSpec, delimiter)[0];
    return rootDir == null ? defaultRootDir : rootDir;
  }

  protected static String getFilterAndDateMark(String collectionSpec, String delimiter) {
    return splitOnLastDelimiter(collectionSpec, delimiter)[1];
  }

  protected static String[] splitOnLastDelimiter(String collectionSpec, String delimiter) {
    if (delimiter == null || delimiter.isEmpty()) {
      return new String[] {null, collectionSpec};
    }

    final String wantSubDirs = delimiter + "**" + delimiter;
    final int startPositionOfLastDelimiter =
        collectionSpec.contains(wantSubDirs) ? collectionSpec.indexOf("/**/") : collectionSpec.lastIndexOf('/');
    final int endPositionOfLastDelimiter =
        collectionSpec.contains(wantSubDirs) ? collectionSpec.indexOf("/**/") + 3 : collectionSpec.lastIndexOf('/');

    if (startPositionOfLastDelimiter == -1) {
      return new String[] {null, collectionSpec.isEmpty() ? null : collectionSpec};
    } else if (endPositionOfLastDelimiter >= collectionSpec.length() - 1) {
      return new String[] {collectionSpec.substring(0, startPositionOfLastDelimiter), null};
    } else {
      return new String[] {collectionSpec.substring(0, startPositionOfLastDelimiter),
          collectionSpec.substring(endPositionOfLastDelimiter + 1)};
    }
  }

  protected static Pattern getRegEx(String filterAndDateMark) {
    if (filterAndDateMark == null) {
      return null;
    }

    int numberOfHashes = filterAndDateMark.length() - filterAndDateMark.replace("#", "").length();

    if (numberOfHashes == 0) {
      return Pattern.compile(filterAndDateMark);
    } else if (numberOfHashes == 1) {
      return Pattern.compile(filterAndDateMark.substring(0, filterAndDateMark.indexOf('#')) + "*");
    } else if (numberOfHashes == 2) {
      final String[] hashSegments = filterAndDateMark.split("#");
      final String dateMarkMatcher = new String(new char[hashSegments[1].length()]).replace("\0", ".");
      return Pattern.compile(hashSegments[0] + dateMarkMatcher + hashSegments[2]);
    }

    throw new IllegalArgumentException("More than two '#' symbols not allowed in spec: " + filterAndDateMark);
  }

  protected static String getDateFormatMark(String filterAndDateMark) {
    if (filterAndDateMark == null) {
      return null;
    }

    int numberOfHashes = filterAndDateMark.length() - filterAndDateMark.replace("#", "").length();

    if (numberOfHashes == 0) {
      return null;
    } else if (numberOfHashes == 1) {
      return filterAndDateMark;
    } else {
      return filterAndDateMark.substring(0, filterAndDateMark.lastIndexOf('#'));
    }
  }

  public PathMatcher getPathMatcher() {
    if (spec.startsWith("regex:") || spec.startsWith("glob:")) { // experimental
      return FileSystems.getDefault().getPathMatcher(spec);
    } else {
      return new BySpecp();
    }
  }

  private class BySpecp implements java.nio.file.PathMatcher {
    @Override
    public boolean matches(Path path) {
      Matcher matcher = filter.matcher(path.getFileName().toString());
      return matcher.matches();
    }
  }

  public String getRootDir() {
    return rootDir;
  }

  /**
   * Get the full path for a file
   * 
   * @param filename the file name
   * @return full file path
   */
  public abstract String getFilePath(String filename);

  public String getDelimiter() {
    return delimiter;
  }

  public String getFragment() {
    return fragment;
  }

  public boolean wantSubdirs() {
    return subdirs;
  }

  public Pattern getFilter() {
    return filter;
  }

  public boolean getFilterOnName() {
    return filterOnName;
  }

  public String getDateFormatMark() {
    return dateFormatMark;
  }

  @Override
  public String toString() {
    return "CollectionSpecParser{" + "\n   topDir='" + rootDir + '\'' + "\n   subdirs=" + subdirs + "\n   regExp='"
        + filter + '\'' + "\n   dateFormatMark='" + dateFormatMark + '\'' +
        // "\n useName=" + useName +
        "\n}";
  }
}
