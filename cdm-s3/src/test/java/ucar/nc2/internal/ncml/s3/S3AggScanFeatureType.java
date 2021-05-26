/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml.s3;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.adapter.DtCoverage;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset.Gridset;
import ucar.nc2.time2.Calendar;
import ucar.nc2.time2.CalendarDate;
import ucar.nc2.time2.CalendarDateRange;
import ucar.unidata.io.s3.S3TestsCommon;

public class S3AggScanFeatureType {

  private static final Logger logger = LoggerFactory.getLogger(S3AggScanFeatureType.class);
  private static final double diffTolerance = 1e-6;

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  @Test
  @Ignore("Not working in 6 - goal should be to get working with new grid classes.")
  public void testAggNewOpenAsDtCoverage() throws IOException {
    String ncmlFile = NcmlTestsCommon.joinNewNcmlScanEnhanced;
    logger.info("Opening {}", ncmlFile);
    DtCoverageDataset cds = DtCoverageDataset.open(ncmlFile);
    checkDtCoverages(cds);
  }

  @Test
  @Ignore("Not working in 6 - goal should be to get working with new grid classes.")
  public void testAggNewOpenNcmlAsDtCoverage() throws IOException {
    String ncml;
    String ncmlFile = NcmlTestsCommon.joinNewNcmlScanEnhanced;
    logger.info("Reading {}", ncmlFile);
    try (Stream<String> ncmlStream = Files.lines(Paths.get(ncmlFile))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();

    logger.debug("...opening contents of {} as string", ncmlFile);
    List<String> names = Arrays.asList(ncmlFile, null);
    for (String name : names) {
      try (NetcdfDataset ncd = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), name, null)) {
        logger.info("...opening NetcdfDataset as DtCoverageDataset using name {}", name);
        DtCoverageDataset cds = new DtCoverageDataset(ncd);
        checkDtCoverages(cds);
      }
    }
  }

  @Test
  @Ignore("Not working in 6 - goal should be to get working with new grid classes.")
  public void testAggNewOpenAsCoverage() throws IOException, InvalidRangeException {
    String ncml;
    String ncmlFile = NcmlTestsCommon.joinNewNcmlScanEnhanced;
    logger.info("Reading {}", ncmlFile);
    try (Stream<String> ncmlStream = Files.lines(Paths.get(ncmlFile))) {
      ncml = ncmlStream.collect(Collectors.joining());
    }
    assertThat(ncml).isNotNull();
    Optional<FeatureDatasetCoverage> fdc = CoverageDatasetFactory.openNcmlString(ncml);
    assertThat(fdc.isPresent()).isTrue();
    List<CoverageCollection> cc = fdc.get().getCoverageCollections();
    checkCoverages(cc);
  }


  private void checkDtCoverages(DtCoverageDataset cds) {
    assertThat(cds).isNotNull();
    List<DtCoverage> grids = cds.getGrids();
    // two agg variables
    assertThat(grids).hasSize(2);
    List<Gridset> gridSets = cds.getGridsets();
    // one coordinate system
    assertThat(gridSets).hasSize(1);
    DtCoverageCS coordSys = gridSets.get(0).getGeoCoordSystem();
    CoordinateAxis timeAxis = coordSys.getTimeAxis();
    assertThat(timeAxis.getSize()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
  }

  private void checkCoverages(List<CoverageCollection> coverageCollections) throws IOException, InvalidRangeException {
    assertThat(coverageCollections).hasSize(1);
    CoverageCollection cc = coverageCollections.get(0);
    // check that ID was picked up from NcML and used for the name
    String name = cc.getName();
    String coverageName = "GOES16 Aggregation for tests";
    assertThat(name).isEqualTo(coverageName);
    Iterable<Coverage> coverages1 = cc.getCoverages();
    List<Coverage> coverages = ImmutableList.copyOf(coverages1);
    // two aggregated variables
    assertThat(coverages).hasSize(2);
    // one coordinate system in the collection
    List<CoverageCoordSys> coordinateSystems = cc.getCoordSys();
    assertThat(coordinateSystems).hasSize(1);
    CoverageCoordSys cs = coordinateSystems.get(0);
    // check that time axis has all times
    CoverageCoordAxis timeAxis = cs.getTimeAxis();
    assertThat(timeAxis.getNcoords()).isEqualTo(NcmlTestsCommon.expectedNumberOfTimesInAgg);
    // get horizontal coordinate system
    HorizCoordSys coverageHcs = cs.getHorizCoordSys();
    CoverageCoordAxis1D xAxis = coverageHcs.getXAxis();
    CoverageCoordAxis1D yAxis = coverageHcs.getYAxis();
    // test subsetting
    SubsetParams subsetParams = new SubsetParams();
    // five minute data, should get three times in subset
    CalendarDate start =
        CalendarDate.fromUdunitIsoDate(Calendar.getDefault().name(), "2017-08-30T00:07:16Z").orElseThrow();
    CalendarDate end =
        CalendarDate.fromUdunitIsoDate(Calendar.getDefault().name(), "2017-08-30T00:17:16Z").orElseThrow();
    // Anticipate: 2017-08-30T00:07:16Z, 2017-08-30T00:12:16Z, 2017-08-30T00:17:16Z
    // long expectedTimesInSubset = end.getDifference(start, Field.Minute) / 5 + 1; LOOK
    CalendarDateRange requestDateRange = CalendarDateRange.of(start, end);
    subsetParams.setTimeRange(requestDateRange);
    // if we don't do a horizontal stride, we will run out of heap
    int horizontalStride = 10;
    subsetParams.setHorizStride(horizontalStride);
    // read subset from single coverage
    Coverage singleCoverage = coverages.get(0);
    GeoReferencedArray subset = singleCoverage.readData(subsetParams);
    // check that time axis was subset
    CoverageCoordSys subsetCoordSys = subset.getCoordSysForData();
    CoverageCoordAxis subsetTimeAxis = subsetCoordSys.getTimeAxis();
    // have to compare the string value of a date range for now
    assertThat(subsetTimeAxis.getDateRange().equals(requestDateRange)).isTrue();
    // assertThat(subsetTimeAxis.getNcoords()).isEqualTo(expectedTimesInSubset); LOOK
    // check horizontal coordinate system
    HorizCoordSys subsetHcs = subsetCoordSys.getHorizCoordSys();
    CoverageCoordAxis1D subsetXAxis = subsetHcs.getXAxis();
    CoverageCoordAxis1D subsetYAxis = subsetHcs.getYAxis();
    assertThat(subsetXAxis.getNcoords()).isEqualTo(xAxis.getNcoords() / horizontalStride);
    assertThat(subsetYAxis.getNcoords()).isEqualTo(yAxis.getNcoords() / horizontalStride);
    checkAxesFirstLastCoordValues(subsetXAxis, xAxis, horizontalStride);
    checkAxesFirstLastCoordValues(subsetYAxis, yAxis, horizontalStride);
  }

  private void checkAxesFirstLastCoordValues(CoverageCoordAxis1D axSub, CoverageCoordAxis axFull, int stride) {
    // check that subset axis has expected number of points
    int expectedNumberOfPoints = axFull.getNcoords() / stride;
    assertThat(axSub.getNcoords()).isEqualTo(expectedNumberOfPoints);
    // check first and last coordinate values of the axis
    Array fullValues = axFull.getCoordsAsArray();
    Array subsetValues = axSub.getCoordsAsArray();
    // check first coordinate value of subset axis
    assertThat(subsetValues.getDouble(0)).isWithin(diffTolerance).of(fullValues.getDouble(0));
    int lastIndexSub = Ints.checkedCast(subsetValues.getSize() - 1);
    int lastIndexFull = Ints.checkedCast(lastIndexSub * stride);
    // check last coordinate value of subset axis
    assertThat(subsetValues.getDouble(lastIndexSub)).isWithin(diffTolerance).of(fullValues.getDouble(lastIndexFull));
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }
}
