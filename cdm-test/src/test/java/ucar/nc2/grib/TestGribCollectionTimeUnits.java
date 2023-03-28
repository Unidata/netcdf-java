/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestGribCollectionTimeUnits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Grib collection with mixture of hours and minutes
  private static final String NAME = "testCollectionUnits";
  private static final String MIXED_UNITS_SPEC =
      TestDir.cdmUnitTestDir + "/tds/ncep/NDFD_NWS_CONUS_conduit_2p5km_#yyyyMMdd_HHmm#\\.grib2$";

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void shouldUseDefaultUnitsInGrib2Collection() throws IOException {
    final FeatureCollectionConfig config = new FeatureCollectionConfig(NAME, NAME, FeatureCollectionType.GRIB2,
        MIXED_UNITS_SPEC, null, null, null, "file", null);

    assertThat(config.gribConfig.getTimeUnitConverter()).isEqualTo(null);
    final double[] valuesInHoursAndMinutes = new double[] {6.0, 12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0,
        66.0, 72.0, 360.0, 720.0, 1080.0, 1440.0, 1800.0, 2160.0, 2520.0, 2880.0, 3240.0, 3600.0, 3960.0, 4320.0};
    checkVariableNameAndTimeAxis(config, "Mixed_intervals", "time3", "Hour", valuesInHoursAndMinutes);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void shouldConvertTimeToMinutesInGrib2Collection() throws IOException {
    final FeatureCollectionConfig config = new FeatureCollectionConfig(NAME, NAME, FeatureCollectionType.GRIB2,
        MIXED_UNITS_SPEC, null, null, null, "file", null);

    setTimeUnitConversionFromHoursToMinutes(config);
    final double[] valuesInMinutes =
        new double[] {360.0, 720.0, 1080.0, 1440.0, 1800.0, 2160.0, 2520.0, 2880.0, 3240.0, 3600.0, 3960.0, 4320.0};
    checkVariableNameAndTimeAxis(config, "360_Minute", "time2", "Minute", valuesInMinutes);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void shouldConvertTimeToHoursInGrib2Collection() throws IOException {
    final FeatureCollectionConfig config = new FeatureCollectionConfig(NAME, NAME, FeatureCollectionType.GRIB2,
        MIXED_UNITS_SPEC, null, null, null, "file", null);

    setTimeUnitConversionFromMinutesToHours(config);
    // hours get rounded down due to refdate being on the half hour
    final double[] valuesInHours = new double[] {5.0, 11.0, 17.0, 23.0, 29.0, 35.0, 41.0, 47.0, 53.0, 59.0, 65.0, 71.0};
    checkVariableNameAndTimeAxis(config, "6_Hour", "time2", "Hour", valuesInHours);
  }

  private void setTimeUnitConversionFromMinutesToHours(FeatureCollectionConfig config) {
    config.gribConfig.addTimeUnitConvert("0", "1");
  }

  private void setTimeUnitConversionFromHoursToMinutes(FeatureCollectionConfig config) {
    config.gribConfig.addTimeUnitConvert("1", "0");
  }

  private void checkVariableNameAndTimeAxis(FeatureCollectionConfig config, String intervalName, String timeAxis,
      String units, double[] values) throws IOException {
    final boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    assertThat(changed).isTrue();
    final String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();

    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(topLevelIndex)) {
      final Variable variable =
          netcdfDataset.findVariable("Best/Total_precipitation_surface_" + intervalName + "_Accumulation");
      assertThat((Iterable<?>) variable).isNotNull();
      assertThat(variable.getDimensionsString()).isEqualTo(timeAxis + " y x");
      final CoordinateAxis coordinateAxis = netcdfDataset.findCoordinateAxis("Best/" + timeAxis);
      assertThat((Iterable<?>) coordinateAxis).isNotNull();
      assertThat(coordinateAxis.getUnitsString()).contains(units);
      assertThat(((CoordinateAxis1D) coordinateAxis).getCoordValues()).isEqualTo(values);
    }
  }
}

