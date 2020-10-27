/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.util.CompareNetcdf2.compareData;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.io.s3.S3TestsCommon;

public class S3AggScan {

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  @Test
  public void testAggNew() throws IOException, InvalidRangeException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(NcmlTestsCommon.joinNewNcmlScan, null)) {
      expectedTimeCheck(ncf);
    }
  }

  @Test
  public void testAggNewOpenNcml() throws IOException, InvalidRangeException {
    String ncml;
    try (Stream<String> ncmlStream = Files.lines(Paths.get(NcmlTestsCommon.joinNewNcmlScan))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();

    try (NetcdfDataset ncd =
        NetcdfDatasets.openNcmlDataset(new StringReader(ncml), NcmlTestsCommon.joinNewNcmlScan, null)) {
      expectedTimeCheck(ncd);
    }
  }

  private void expectedTimeCheck(NetcdfFile ncd) throws IOException, InvalidRangeException {
    // see https://noaa-goes16.s3.amazonaws.com/index.html#ABI-L1b-RadC/2017/242/00/
    // we are grabbing OR_ABI-L1b-RadC-M3C01_G16_s*
    int[] expectedShape = new int[] {NcmlTestsCommon.expectedNumberOfTimesInAgg, 3000, 5000};

    Dimension time = ncd.findDimension(NcmlTestsCommon.timeVarName);
    assertThat(time).isNotNull();
    assertThat(time.getLength()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
    Variable timeVar = ncd.findVariable(NcmlTestsCommon.timeVarName);
    Array timeValues = timeVar.read();
    assertThat(timeValues).isNotNull();
    Variable expectedTimeVar = ncd.findVariable("/expected_time");
    Array expectedTimeValues = expectedTimeVar.read();
    assertThat(expectedTimeValues).isNotNull();
    assertThat(compareData(NcmlTestsCommon.timeVarName, timeValues, expectedTimeValues)).isTrue();

    // try to read from first and last object
    Variable dataVar = ncd.findVariable(NcmlTestsCommon.dataVarName);
    Assert.assertNotNull(dataVar);
    int[] shape = dataVar.getShape();
    assertThat(shape).isEqualTo(expectedShape);
    Array data1 = dataVar.read("0,:,:");
    Array data2 = dataVar.read(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:");
    assertThat(data1.getDouble(0)).isNotWithin(1e-6).of(data2.getDouble(0));
  }

  @Test
  public void compareExplicitVsScan() throws IOException, InvalidRangeException {
    // 5 minute scans
    try (NetcdfFile ncfScan = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlScan);
        NetcdfFile ncfExp = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit)) {
      // read Rad data from first and last time
      Variable dataVarScan = ncfScan.findVariable(NcmlTestsCommon.dataVarName);
      Variable dataVarExp = ncfExp.findVariable(NcmlTestsCommon.dataVarName);
      Assert.assertNotNull(dataVarScan);
      Assert.assertNotNull(dataVarExp);
      Array dataScanFirst = dataVarScan.read("0,:,:");
      Array dataScanLast = dataVarScan.read(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:");
      Array dataExpFirst = dataVarExp.read("0,:,:");
      Array dataExpLast = dataVarExp.read(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:");

      // compare data from the scan aggregation and the explicit aggregation
      assertThat(compareData(NcmlTestsCommon.dataVarName, dataScanFirst, dataExpFirst)).isTrue();
      assertThat(compareData(NcmlTestsCommon.dataVarName, dataScanLast, dataExpLast)).isTrue();
    }
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
