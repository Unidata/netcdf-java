/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.category.Slow;

public class TestS3AggScanFeatureType {

  private static final Logger logger = LoggerFactory.getLogger(TestS3AggScanFeatureType.class);
  private static final double diffTolerance = 1e-5;
  private static final String ncml = NcmlTestsCommon.joinNewNcmlScanEnhanced;

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  @Test
  public void testConstructCoordinateSystems() throws IOException {
    System.out.printf("testConstructCoordinateSystems %s%n", ncml);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(ncml)) {
      Variable v = ncd.findVariable("Rad");
      assertThat(v).isNotNull();
      assertThat(v).isInstanceOf(VariableEnhanced.class);
      VariableEnhanced ve = (VariableEnhanced) v;
      assertThat(ve.getCoordinateSystems()).isNotEmpty();
    }
  }

  @Test
  public void testOpenGridDataset() throws IOException {
    String ncmlFile = NcmlTestsCommon.joinNewNcmlScanEnhanced;
    File file = new File(ncmlFile);
    System.out.printf("testOpenGridDataset %s%n", file.getAbsolutePath());
    Formatter errlog = new Formatter();
    try (GridDataset cds = GridDatasetFactory.openGridDataset(ncmlFile, errlog)) {
      if (cds == null) {
        System.out.printf("err=%s%n", errlog);
      } else {
        checkGridDataset(cds);
      }
    }
  }

  @Test
  @Category(Slow.class)
  public void testWrapGridDataset() throws IOException, InvalidRangeException {
    String ncml;
    String ncmlFile = NcmlTestsCommon.joinNewNcmlScanEnhanced;
    System.out.printf("testWrapGridDataset %s%n", ncmlFile);
    try (Stream<String> ncmlStream = Files.lines(Paths.get(ncmlFile))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();

    List<String> names = Arrays.asList(ncmlFile, null);
    for (String name : names) {
      Formatter errlog = new Formatter();
      try (NetcdfDataset ncd = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), name, null);
          GridDataset gds = GridDatasetFactory.wrapGridDataset(ncd, errlog).orElseThrow()) {
        assertThat(gds).isNotNull();
        checkGridDataset(gds);
        checkCoverages(gds.getGrids());
      }
    }
  }

  private void checkGridDataset(GridDataset gds) {
    assertThat(gds).isNotNull();
    List<Grid> grids = gds.getGrids();
    // two agg variables
    assertThat(grids).hasSize(2);

    // one coordinate system
    assertThat(gds.getGridCoordinateSystems()).hasSize(1);
    GridCoordinateSystem coordSys = grids.get(0).getCoordinateSystem();
    GridAxis<?> timeAxis = coordSys.findCoordAxisByType(AxisType.Time);
    assertThat((Object) timeAxis).isNotNull();
    assertThat(timeAxis.getNominalSize()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
  }

  private void checkCoverages(List<Grid> grids) throws IOException, InvalidRangeException {
    assertThat(grids).hasSize(2);
    Grid grid = grids.get(0);
    GridCoordinateSystem coordSys = grid.getCoordinateSystem();

    // get horizontal coordinate system
    GridHorizCoordinateSystem hcs = coordSys.getHorizCoordinateSystem();
    GridAxisPoint xAxis = hcs.getXHorizAxis();
    GridAxisPoint yAxis = hcs.getYHorizAxis();

    //// test subsetting

    // five minute data, should get 1, 2, and 3 in the subset:
    // "2017-08-30T00:02:16Z", "2017-08-30T00:07:16Z", "2017-08-30T00:12:16Z", "2017-08-30T00:17:16Z",
    // "2017-08-30T00:22:16Z",
    // "2017-08-30T00:27:16Z", "2017-08-30T00:32:16Z", "2017-08-30T00:37:16Z", "2017-08-30T00:42:16Z",
    // "2017-08-30T00:47:16Z",
    // "2017-08-30T00:52:16Z", "2017-08-30T00:57:16Z"
    CalendarDate start =
        CalendarDate.fromUdunitIsoDate(Calendar.getDefault().name(), "2017-08-30T00:07:16Z").orElseThrow();
    CalendarDate end =
        CalendarDate.fromUdunitIsoDate(Calendar.getDefault().name(), "2017-08-30T00:17:16Z").orElseThrow();
    CalendarDateRange requestDateRange = CalendarDateRange.of(start, end);

    // if we don't do a horizontal stride, we will run out of heap
    int horizontalStride = 10;
    GridReferencedArray subset =
        grid.getReader().setDateRange(requestDateRange).setHorizStride(horizontalStride).read();
    MaterializedCoordinateSystem subsetCoordSys = subset.getMaterializedCoordinateSystem();
    GridTimeCoordinateSystem tcs = subsetCoordSys.getTimeCoordSystem();
    assertThat(tcs).isNotNull();

    // check that time axis was subset
    GridAxis<?> subsetTimeAxis = tcs.getTimeOffsetAxis(0);
    assertThat((Object) subsetTimeAxis).isNotNull();

    // compare date range for now
    // Anticipate: 2017-08-30T00:07:16Z, 2017-08-30T00:12:16Z, 2017-08-30T00:17:16Z
    CalendarDateUnit cdu = tcs.makeOffsetDateUnit(0);
    CalendarDate startDate = cdu.makeFractionalCalendarDate(subsetTimeAxis.getCoordDouble(0));
    assertThat(startDate).isEqualTo(start);
    CalendarDate endate =
        cdu.makeFractionalCalendarDate(subsetTimeAxis.getCoordDouble(subsetTimeAxis.getNominalSize() - 1));
    assertThat(endate).isEqualTo(end);

    // check horizontal coordinate system
    GridHorizCoordinateSystem subsetHcs = subsetCoordSys.getHorizCoordinateSystem();
    GridAxisPoint subsetXAxis = subsetHcs.getXHorizAxis();
    GridAxisPoint subsetYAxis = subsetHcs.getYHorizAxis();

    assertThat(subsetXAxis.getNominalSize()).isEqualTo(xAxis.getNominalSize() / horizontalStride);
    assertThat(subsetYAxis.getNominalSize()).isEqualTo(yAxis.getNominalSize() / horizontalStride);
    checkAxesFirstLastCoordValues(subsetXAxis, xAxis, horizontalStride);
    checkAxesFirstLastCoordValues(subsetYAxis, yAxis, horizontalStride);
  }

  private void checkAxesFirstLastCoordValues(GridAxisPoint axSub, GridAxisPoint axFull, int stride) {
    // check that subset axis has expected number of points
    int expectedNumberOfPoints = axFull.getNominalSize() / stride;
    assertThat(axSub.getNominalSize()).isEqualTo(expectedNumberOfPoints);

    // check first and last coordinate values of the axis
    assertThat(axSub.getCoordDouble(0)).isWithin(diffTolerance).of(axFull.getCoordDouble(0));
    // last one has to be just within the subsetted grid resolution
    assertThat(axSub.getCoordDouble(axSub.getNominalSize() - 1)).isWithin(Math.abs(axSub.getResolution()))
        .of(axFull.getCoordDouble(axFull.getNominalSize() - 1));
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
