/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.TestDir;

public class NcmlTestsCommon {
  ///////////////////////////
  // comparable aggregations
  // explicitly aggregate individual objects
  static final String joinNewNcmlExplicit = TestDir.localTestDataDir + "ncml/joinNewExplicit/S3JoinNewAggExplicit.ncml";
  // join new, scan
  // extract date from object name, filter object path on suffix
  static final String joinNewNcmlScanSuffixDelim = TestDir.localTestDataDir + "ncml/joinNewScan/suffix/S3Delim.ncml";
  static final String joinNewNcmlScanSuffixNoDelim =
      TestDir.localTestDataDir + "ncml/joinNewScan/suffix/S3NoDelim.ncml";
  // same, but add enhance="all" to netcdf element
  static final String joinNewNcmlScanEnhanced =
      TestDir.localTestDataDir + "ncml/joinNewScan/suffix/S3DelimEnhanced.ncml";
  // join new, scan
  // extract date from object name, filter object name using regular expression
  static final String joinNewNcmlScanRegExpDelim = TestDir.localTestDataDir + "ncml/joinNewScan/regExp/S3Delim.ncml";
  static final String joinNewNcmlScanRegExpNoDelim =
      TestDir.localTestDataDir + "ncml/joinNewScan/regExp/S3NoDelim.ncml";
  // join new, scan
  // promote global attribute in each component file to a variable to create time variable, filter filter object path
  // on suffix
  static final String joinNewNcmlScanPromoteDelim = TestDir.localTestDataDir + "ncml/joinNewScan/promote/S3Delim.ncml";
  static final String joinNewNcmlScanPromoteNoDelim =
      TestDir.localTestDataDir + "ncml/joinNewScan/promote/S3NoDelim.ncml";

  static final int expectedNumberOfTimesInAgg = 12;
  static final int expectedNumberOfTimesInAggRegExp = 2;
  static final String dataVarName = "/Rad";
  static final String timeDimName = "/time";
  static final String timeVarName = "/time";
  static final String timeVarNamePromote = "/times";
  static final String expectedTimeVarName = "/expected_time";

  static final String firstObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
  static final String lastObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420057168_e20172420059540_c20172420059581.nc";
  static final String sixthObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420027168_e20172420029540_c20172420029583.nc";
}
