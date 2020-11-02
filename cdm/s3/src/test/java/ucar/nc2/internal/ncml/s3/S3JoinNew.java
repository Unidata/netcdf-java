/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.util.CompareNetcdf2.compareData;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.time.CalendarPeriod.Field;
import ucar.unidata.io.s3.S3TestsCommon;

public class S3JoinNew {

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  @Test
  public void testJoinNewFile() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(NcmlTestsCommon.joinNewNcmlExplicit, null)) {
      Dimension time = ncf.findDimension(NcmlTestsCommon.timeVarName);
      assertThat(time).isNotNull();
      assertThat(time.getLength()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
      Variable timeVar = ncf.findVariable(NcmlTestsCommon.timeVarName);
      Array timeValues = timeVar.read();
      assertThat(timeValues).isNotNull();
      for (int i = 0; i < timeValues.getSize(); i++) {
        assertThat(timeValues.getInt(i)).isEqualTo(2 + i * 5);
      }
    }
  }

  @Test
  public void testJoinNewOpenNcml() throws IOException {
    String ncml;
    try (Stream<String> ncmlStream = Files.lines(Paths.get(NcmlTestsCommon.joinNewNcmlExplicit))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();

    try (NetcdfDataset ncd =
        NetcdfDatasets.openNcmlDataset(new StringReader(ncml), NcmlTestsCommon.joinNewNcmlExplicit, null)) {
      Dimension time = ncd.findDimension(NcmlTestsCommon.timeVarName);
      assertThat(time).isNotNull();
      assertThat(time.getLength()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
      Variable timeVar = ncd.findVariable(NcmlTestsCommon.timeVarName);
      Array timeValues = timeVar.read();
      assertThat(timeValues).isNotNull();
      for (int i = 0; i < timeValues.getSize(); i++) {
        assertThat(timeValues.getInt(i)).isEqualTo(2 + i * 5);
      }
    }
  }

  @Test
  public void testJoinNewDataset() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit)) {
      Variable rad = ncd.findVariable(NcmlTestsCommon.dataVarName);
      ImmutableList<CoordinateSystem> coordSystems = ((VariableDS) rad).getCoordinateSystems();
      assertThat(coordSystems).hasSize(1);
      CoordinateSystem cs = coordSystems.get(0);
      CoordinateAxis timeAxis = cs.getTaxis();
      assertThat(timeAxis instanceof CoordinateAxis1D).isTrue();
      CoordinateAxis1DTime timeAxis1d = CoordinateAxis1DTime.factory(ncd, timeAxis, null);
      CalendarDate baseDate = CalendarDate.parseISOformat(Calendar.gregorian.name(), "2017-08-30 00:00");
      List<CalendarDate> aggCalDates = timeAxis1d.getCalendarDates();
      for (int i = 0; i < aggCalDates.size(); i++) {
        assertThat(aggCalDates.get(i)).isEqualTo(baseDate.add(CalendarPeriod.of(2 + i * 5, Field.Minute)));
      }
    }
  }

  @Test
  public void testCompareFirstAndLast() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncdAgg = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit);
        NetcdfDataset ncdFirst = NetcdfDatasets.openDataset(NcmlTestsCommon.firstObjectLocation);
        NetcdfDataset ncdLast = NetcdfDatasets.openDataset(NcmlTestsCommon.lastObjectLocation);
        NetcdfDataset ncdMid = NetcdfDatasets.openDataset(NcmlTestsCommon.sixthObjectLocation)) {

      Variable varAggDs = ncdAgg.findVariable(NcmlTestsCommon.dataVarName);
      Variable varFirstDs = ncdFirst.findVariable(NcmlTestsCommon.dataVarName);
      Variable varMidDs = ncdMid.findVariable(NcmlTestsCommon.dataVarName);
      Variable varLastDs = ncdLast.findVariable(NcmlTestsCommon.dataVarName);

      Array dataAggFirst = varAggDs.read("0,:,:");
      Array dataAggMid = varAggDs.read((NcmlTestsCommon.expectedNumberOfTimesInAgg / 2) - 1 + ",:,:");
      Array dataAggLast = varAggDs.read(NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:");

      Array dataFirst = varFirstDs.read();
      Array dataMid = varMidDs.read();
      Array dataLast = varLastDs.read();

      // same total number of elements?
      assertThat(dataAggFirst.getSize()).isEqualTo(dataFirst.getSize());
      assertThat(dataAggMid.getSize()).isEqualTo(dataMid.getSize());
      assertThat(dataAggLast.getSize()).isEqualTo(dataLast.getSize());

      // same shapes (agg array must be reduced to get rid of the single time dimension)
      assertThat(dataAggFirst.getShape()).isNotEqualTo(dataFirst.getShape());
      assertThat(dataAggMid.getShape()).isNotEqualTo(dataMid.getShape());
      assertThat(dataAggLast.getShape()).isNotEqualTo(dataLast.getShape());
      assertThat(dataAggFirst.reduce().getShape()).isEqualTo(dataFirst.getShape());
      assertThat(dataAggMid.reduce().getShape()).isEqualTo(dataMid.getShape());
      assertThat(dataAggLast.reduce().getShape()).isEqualTo(dataLast.getShape());

      // compare data arrays
      assertThat(compareData(NcmlTestsCommon.dataVarName, dataAggFirst.reduce(), dataFirst)).isTrue();
      assertThat(compareData(NcmlTestsCommon.dataVarName, dataAggMid.reduce(), dataMid)).isTrue();
      assertThat(compareData(NcmlTestsCommon.dataVarName, dataAggLast.reduce(), dataLast)).isTrue();
    }
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
