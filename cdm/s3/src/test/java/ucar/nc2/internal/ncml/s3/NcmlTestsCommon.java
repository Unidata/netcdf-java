/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.TestDir;

public class NcmlTestsCommon {
  // comparable aggregations
  static final String joinNewNcmlExplicit = TestDir.localTestDataDir + "ncml/S3JoinNewAggExplicit.ncml";
  static final String joinNewNcmlScan = TestDir.localTestDataDir + "ncml/S3JoinNewAggScan.ncml";
  static final int expectedNumberOfTimesInAgg = 12;
  static final String dataVarName = "/Rad";
  static final String timeVarName = "/time";

  static final String firstObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
  static final String lastObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420057168_e20172420059540_c20172420059581.nc";
  static final String sixthObjectLocation = S3TestsCommon.TOP_LEVEL_AWS_BUCKET
      + "?ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420027168_e20172420029540_c20172420029583.nc";
}
