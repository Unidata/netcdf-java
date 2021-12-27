/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.internal.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.calendar.CalendarPeriod.Field;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.category.Slow;

@Category(Slow.class)
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
      Array<Number> timeValues = (Array<Number>) timeVar.readArray();
      assertThat(timeValues).isNotNull();
      for (int i = 0; i < timeValues.getSize(); i++) {
        assertThat(timeValues.get(i)).isEqualTo(2 + i * 5);
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
      Array<Number> timeValues = (Array<Number>) timeVar.readArray();
      assertThat(timeValues).isNotNull();
      for (int i = 0; i < timeValues.getSize(); i++) {
        assertThat(timeValues.get(i)).isEqualTo(2 + i * 5);
      }
    }
  }

  @Test
  @Ignore("Not working in 6 - goal should be to get working with new grid axes classes.")
  public void testJoinNewDataset() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit)) {
      Variable rad = ncd.findVariable(NcmlTestsCommon.dataVarName);
      List<CoordinateSystem> coordSystems = ((VariableDS) rad).getCoordinateSystems();
      assertThat(coordSystems).hasSize(1);
      CoordinateSystem cs = coordSystems.get(0);
      CoordinateAxis timeAxis = cs.findAxis(AxisType.Time);
      assertThat(timeAxis instanceof CoordinateAxis1D).isTrue();
      CoordinateAxis1DTime timeAxis1d = CoordinateAxis1DTime.factory(ncd, timeAxis, null);
      CalendarDate baseDate =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.name(), "2017-08-30 00:00").orElseThrow();
      List<CalendarDate> aggCalDates = timeAxis1d.getCalendarDates();
      for (int i = 0; i < aggCalDates.size(); i++) {
        assertThat(aggCalDates.get(i)).isEqualTo(baseDate.add(CalendarPeriod.of(2 + i * 5, Field.Minute)));
      }
    }
  }

  @Test
  @Category(Slow.class)
  public void testCompareFirstAndLast() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncdAgg = NetcdfDatasets.openDataset(NcmlTestsCommon.joinNewNcmlExplicit);
        NetcdfDataset ncdFirst = NetcdfDatasets.openDataset(NcmlTestsCommon.firstObjectLocation);
        NetcdfDataset ncdLast = NetcdfDatasets.openDataset(NcmlTestsCommon.lastObjectLocation);
        NetcdfDataset ncdMid = NetcdfDatasets.openDataset(NcmlTestsCommon.sixthObjectLocation)) {

      Variable varFirstDs = ncdFirst.findVariable(NcmlTestsCommon.dataVarName);
      Variable varMidDs = ncdMid.findVariable(NcmlTestsCommon.dataVarName);
      Variable varLastDs = ncdLast.findVariable(NcmlTestsCommon.dataVarName);

      Variable varAggDs = ncdAgg.findVariable(NcmlTestsCommon.dataVarName);
      String aggFirstSpec = "0,:,:";
      String aggMidSpec = (NcmlTestsCommon.expectedNumberOfTimesInAgg / 2) - 1 + ",:,:";
      String aggLastSpec = NcmlTestsCommon.expectedNumberOfTimesInAgg - 1 + ",:,:";

      compare(varFirstDs, varAggDs, aggFirstSpec);
      compare(varMidDs, varAggDs, aggMidSpec);
      compare(varLastDs, varAggDs, aggLastSpec);
    }
  }

  private void compare(Variable single, Variable agg, String aggSectionSpec) throws IOException, InvalidRangeException {
    // read data to compare
    Array dataSingle = single.readArray();
    Array dataAgg = agg.readArray(new Section(aggSectionSpec));

    // same total number of elements?
    assertThat(dataSingle.getSize()).isEqualTo(dataAgg.getSize());

    // same shapes (agg array must be reduced to get rid of the single time dimension)
    assertThat(dataAgg.getShape()).isNotEqualTo(dataSingle.getShape());
    assertThat(Arrays.reduce(dataAgg).getShape()).isEqualTo(dataSingle.getShape());

    // compare data arrays
    assertThat(CompareArrayToArray.compareData(NcmlTestsCommon.dataVarName, Arrays.reduce(dataAgg), dataSingle))
        .isTrue();
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
