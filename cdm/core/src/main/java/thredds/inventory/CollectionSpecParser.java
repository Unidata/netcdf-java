/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import java.io.File;
import java.util.Formatter;

/**
 * Parses the collection specification string for local files.
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
public class CollectionSpecParser extends CollectionSpecParserAbstract {
  private final static String DELIMITER = "/";
  private final static String FRAGMENT = "";
  private final static String DEFAULT_DIR = System.getProperty("user.dir");

  /**
   * Single spec : "/topdir/** /#dateFormatMark#regExp"
   * This only allows the dateFormatMark to be in the file name, not anywhere else in the filename path,
   * and you can't use any part of the dateFormat to filter on.
   *
   * @param collectionSpec the collection spec
   * @param errlog put error messages here, may be null
   */
  public CollectionSpecParser(String collectionSpec, Formatter errlog) {
    super(collectionSpec, getRootDir(collectionSpec, DEFAULT_DIR, DELIMITER),
        getFilterAndDateMark(collectionSpec, DELIMITER), DELIMITER, FRAGMENT, errlog);

    File locFile = new File(rootDir);
    if (!locFile.exists() && errlog != null) {
      errlog.format(" Directory %s does not exist %n", rootDir);
    }
  }

  /**
   * @param rootDir the root directory
   * @param regExp the regular expression to use as a filter
   * @param errlog put error messages here, may be null
   */
  public CollectionSpecParser(String rootDir, String regExp, Formatter errlog) {
    super(rootDir, regExp, DELIMITER, FRAGMENT, errlog);
  }

  @Override
  public String getFilePath(String filename) {
    return rootDir.endsWith(delimiter) ? rootDir + filename : rootDir + delimiter + filename;
  }
}
