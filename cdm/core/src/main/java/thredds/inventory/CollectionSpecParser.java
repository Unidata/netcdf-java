/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ucar.unidata.util.StringUtil2;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Formatter;

/**
 * Parses the collection specification string.
 * <p>
 * the idea is that one copies the full path of an example dataset, then edits it
 * </p>
 * <p>
 * Example: "/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/** /GFS_Alaska_191km_#yyyyMMdd_HHmm#\.grib1$"
 * </p>
 * <ul>
 * <li>rootDir ="/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km"/</li>
 * <li>subdirs=true (because ** is present)</li>
 * <li>dateFormatMark="GFS_Alaska_191km_#yyyyMMdd_HHmm"</li>
 * <li>regExp='GFS_Alaska_191km_.............\.grib1$</li>
 * </ul>
 * <p>
 * Example: "Q:/grid/grib/grib1/data/agg/.*\.grb"
 * </p>
 * <ul>
 * <li>rootDir ="Q:/grid/grib/grib1/data/agg/"/</li>
 * <li>subdirs=false</li>
 * <li>dateFormatMark=null</li>
 * <li>useName=yes</li>
 * <li>regexp= ".*\.grb" (anything ending with .grb)</li>
 * </ul>
 *
 * @see "https://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.2/reference/collections/CollectionSpecification.html"
 * @author caron
 * @since Jul 7, 2009
 */
@ThreadSafe
public class CollectionSpecParser {
  private final String spec;
  private final String rootDir;
  private final boolean subdirs; // recurse into subdirectories under the root dir
  private final boolean filterOnName; // filter on name, else on entire path
  private final Pattern filter; // regexp filter
  private final String dateFormatMark;

  /**
   * Single spec : "/topdir/** /#dateFormatMark#regExp"
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
   * and you cant use any part of the dateFormat to filter on.
   * 
   * @param collectionSpec the collection Spec
   * @param errlog put error messages here, may be null
   */
  public CollectionSpecParser(String collectionSpec, Formatter errlog) {
    this.spec = collectionSpec.trim();

    rootDir = getRootDir(collectionSpec);
    subdirs = collectionSpec.contains("/**/");
    final String filterAndDateMark = getFilterAndDateMarkString(collectionSpec);
    filter = getRegEx(filterAndDateMark);
    dateFormatMark = getDateFormatMark(filterAndDateMark);

    File locFile = new File(rootDir);
    if (!locFile.exists() && errlog != null) {
      errlog.format(" Directory %s does not exist %n", rootDir);
    }

    this.filterOnName = true;
  }

  public CollectionSpecParser(String rootDir, String regExp, Formatter errlog) {
    this.rootDir = StringUtil2.removeFromEnd(rootDir, '/');
    this.subdirs = true;
    this.spec = this.rootDir + "/" + regExp;
    this.filter = Pattern.compile(spec);
    this.dateFormatMark = null;
    this.filterOnName = false;
  }

  private static String getRootDir(String collectionSpec) {
    if (collectionSpec.contains("/**/")) {
      return collectionSpec.split(Pattern.quote("/**/"))[0];
    }

    if (collectionSpec.contains("/")) {
      return collectionSpec.substring(0, collectionSpec.lastIndexOf('/'));
    }

    return System.getProperty("user.dir"); // working directory
  }

  private static String getFilterAndDateMarkString(String collectionSpec) {
    final int posFilter =
        collectionSpec.contains("/**/") ? collectionSpec.indexOf("/**/") + 3 : collectionSpec.lastIndexOf('/');

    if (posFilter >= collectionSpec.length() - 2) {
      return null;
    }

    return collectionSpec.substring(posFilter + 1); // remove topDir
  }

  private static Pattern getRegEx(String filterAndDateMark) {
    if (filterAndDateMark == null) {
      return null;
    }

    int numberOfHashes = filterAndDateMark.length() - filterAndDateMark.replace("#", "").length();

    if (numberOfHashes == 0) {
      return Pattern.compile(filterAndDateMark);
    } else if (numberOfHashes == 1) {
      return Pattern.compile(filterAndDateMark.substring(0, filterAndDateMark.indexOf('#')) + "*");
    } else {
      StringBuilder sb = new StringBuilder(StringUtil2.remove(filterAndDateMark, '#')); // remove hashes, replace with .

      for (int i = filterAndDateMark.indexOf('#'); i < filterAndDateMark.lastIndexOf('#') - 1; i++) {
        sb.setCharAt(i, '.');
      }

      return Pattern.compile(sb.toString());
    }
  }

  private static String getDateFormatMark(String filterAndDateMark) {
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
