/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.category.Slow;

@Category(Slow.class)
public class S3AggScan {

  private static final Logger logger = LoggerFactory.getLogger(S3AggScan.class);

  private final String[] scanWithSuffix =
      new String[] {NcmlTestsCommon.joinNewNcmlScanSuffixDelim, NcmlTestsCommon.joinNewNcmlScanSuffixNoDelim};

  private final String[] scanWithRegExp =
      new String[] {NcmlTestsCommon.joinNewNcmlScanRegExpDelim, NcmlTestsCommon.joinNewNcmlScanRegExpNoDelim};

  private final String[] scanWithPromote =
      new String[] {NcmlTestsCommon.joinNewNcmlScanPromoteDelim, NcmlTestsCommon.joinNewNcmlScanPromoteNoDelim};

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  @Test
  public void testAggJoinNewSuffix() throws Exception {
    for (String ncmlFile : scanWithSuffix) {
      logger.info("Opening {}", ncmlFile);
      try (NetcdfFile ncf = NetcdfDatasets.openFile(ncmlFile, null)) {
        expectedTimeCheck(ncf, NcmlTestsCommon.expectedNumberOfTimesInAgg);
      }
    }
  }

  @Test
  public void testAggJoinNewPromote() throws Exception {
    for (String ncmlFile : scanWithPromote) {
      logger.info("Opening {}", ncmlFile);
      try (NetcdfFile ncf = NetcdfDatasets.openFile(ncmlFile, null)) {
        expectedTimeCheck(ncf, NcmlTestsCommon.expectedNumberOfTimesInAgg, NcmlTestsCommon.timeVarNamePromote);
      }
    }
  }

  @Test
  public void testAggJoinNewRegExp() throws Exception {
    for (String ncmlFile : scanWithRegExp) {
      logger.info("Opening {}", ncmlFile);
      try (NetcdfFile ncf = NetcdfDatasets.openFile(ncmlFile, null)) {
        expectedTimeCheck(ncf, NcmlTestsCommon.expectedNumberOfTimesInAggRegExp);
      }
    }
  }

  @Test
  public void testAggJoinNewSuffixOpenNcml() throws Exception {
    for (String ncmlFile : scanWithSuffix) {
      checkOpenNcmlFromString(ncmlFile, NcmlTestsCommon.expectedNumberOfTimesInAgg);
    }
  }

  @Test
  public void testAggJoinNewPromoteOpenNcml() throws Exception {
    for (String ncmlFile : scanWithPromote) {
      checkOpenNcmlFromString(ncmlFile, NcmlTestsCommon.expectedNumberOfTimesInAgg, NcmlTestsCommon.timeVarNamePromote);
    }
  }

  @Test
  public void testAggJoinNewRegExpOpenNcml() throws IOException, Exception {
    for (String ncmlFile : scanWithRegExp) {
      checkOpenNcmlFromString(ncmlFile, NcmlTestsCommon.expectedNumberOfTimesInAggRegExp);
    }
  }

  @Test
  public void compareExplicitVsScan() throws Exception {
    // 5 minute scans
    try (NetcdfFile ncfScan = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlScanSuffixDelim);
        NetcdfFile ncfExp = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit)) {
      // read Rad data from first and last time
      Variable dataVarScan = ncfScan.findVariable(NcmlTestsCommon.dataVarName);
      Variable dataVarExp = ncfExp.findVariable(NcmlTestsCommon.dataVarName);
      assertThat(dataVarScan).isNotNull();
      assertThat(dataVarExp).isNotNull();
      Array<?> dataScanFirst = dataVarScan.readArray(new Section("0,:,:"));
      Array<?> dataScanLast =
          dataVarScan.readArray(new Section(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:"));
      Array<?> dataExpFirst = dataVarExp.readArray(new Section("0,:,:"));
      Array<?> dataExpLast = dataVarExp.readArray(new Section(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:"));

      // compare data from the scan aggregation and the explicit aggregation
      assertThat(CompareArrayToArray.compareData(NcmlTestsCommon.dataVarName, dataScanFirst, dataExpFirst)).isTrue();
      assertThat(CompareArrayToArray.compareData(NcmlTestsCommon.dataVarName, dataScanLast, dataExpLast)).isTrue();
    }
  }

  private void expectedTimeCheck(NetcdfFile ncd, int expectedTimeDimSize) throws IOException, InvalidRangeException {
    expectedTimeCheck(ncd, expectedTimeDimSize, NcmlTestsCommon.timeVarName);
  }

  private void expectedTimeCheck(NetcdfFile ncd, int expectedTimeDimSize, String timeVarName)
      throws IOException, InvalidRangeException {
    // see https://noaa-goes16.s3.amazonaws.com/index.html#ABI-L1b-RadC/2017/242/00/
    // we are grabbing OR_ABI-L1b-RadC-M3C01_G16_s*
    logger.debug("...Checking {}", ncd.getLocation());
    int[] expectedShape = new int[] {expectedTimeDimSize, 3000, 5000};

    Dimension time = ncd.findDimension("/" + NcmlTestsCommon.timeDimName);
    assertThat(time).isNotNull();
    assertThat(time.getLength()).isEqualTo(expectedTimeDimSize);
    Variable timeVar = ncd.findVariable(timeVarName);
    Array<?> timeValues = timeVar.readArray();
    assertThat(timeValues).isNotNull();
    Variable expectedTimeVar = ncd.findVariable(NcmlTestsCommon.expectedTimeVarName);
    Array<?> expectedTimeValues = expectedTimeVar.readArray();
    assertThat(expectedTimeValues).isNotNull();
    assertThat(CompareArrayToArray.compareData(timeVarName, timeValues, expectedTimeValues)).isTrue();

    // try to read from first and last object
    Variable dataVar = ncd.findVariable(NcmlTestsCommon.dataVarName);
    assertThat(dataVar).isNotNull();
    int[] shape = dataVar.getShape();
    assertThat(shape).isEqualTo(expectedShape);
    Array<Number> data1 = (Array<Number>) dataVar.readArray(new Section("0,0,0"));
    Array<Number> data2 = (Array<Number>) dataVar.readArray(new Section(expectedTimeDimSize - 1 + ",0,0"));
    assertThat(data1.getScalar()).isNotEqualTo(data2.getScalar());

    // make sure coordinates attribute contains the time var
    assertThat(dataVar.attributes().findAttribute(CF.COORDINATES).toString()).contains(timeVarName.replace("/", ""));
  }

  private void checkOpenNcmlFromString(String ncmlFile, int expectedNumberOfTimes)
      throws IOException, InvalidRangeException {
    checkOpenNcmlFromString(ncmlFile, expectedNumberOfTimes, NcmlTestsCommon.timeVarName);
  }

  private void checkOpenNcmlFromString(String ncmlFile, int expectedNumberOfTimes, String timeVarName)
      throws IOException, InvalidRangeException {
    String ncml;
    logger.info("Reading {}", ncmlFile);
    try (Stream<String> ncmlStream = Files.lines(Paths.get(ncmlFile))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();

    logger.debug("...opening contents of {} as string", ncmlFile);
    try (NetcdfDataset ncd = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), ncmlFile, null)) {
      expectedTimeCheck(ncd, expectedNumberOfTimes, timeVarName);
    }
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
