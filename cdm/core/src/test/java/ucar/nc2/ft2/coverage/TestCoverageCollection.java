package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCoordAxis.Spacing;

public class TestCoverageCollection {
  private final AttributeContainerMutable attributes = new AttributeContainerMutable("myAttributes");

  @Test
  public void shouldCreateCoverageCollection() {
    List<CoverageCoordSys> coordSystems =
        Arrays.asList(new CoverageCoordSys("myCoordinateSystem1", Arrays.asList("lat", "lon"), null, FeatureType.GRID),
            new CoverageCoordSys("myCoordinateSystem2", Arrays.asList("time", "lon", "lat"), null, FeatureType.GRID));

    List<CoverageCoordAxis> coordAxes =
        Arrays.asList(makeAxis("lat", AxisType.Lat), makeAxis("lon", AxisType.Lon), makeAxis("time", AxisType.Time));

    Coverage coverage1 =
        new Coverage("name1", DataType.INT, attributes, "myCoordinateSystem1", "units", "description", null, null);
    Coverage coverage2 =
        new Coverage("name2", DataType.INT, attributes, "myCoordinateSystem2", "units", "description", null, null);
    List<Coverage> coverages = Arrays.asList(coverage1, coverage2);

    CoverageCollection coverageCollection = new CoverageCollection("name", FeatureType.GRID, null, null, null, null,
        coordSystems, null, coordAxes, coverages, null);
    assertThat(coverageCollection).isNotNull();
  }

  @Test
  public void shouldRefuseZeroHorizontalCoordinateSystems() {
    List<CoverageCoordSys> coordSys = Collections.singletonList(
        new CoverageCoordSys("myCoordinateSystem1", Collections.singletonList("time"), null, FeatureType.GRID));

    List<CoverageCoordAxis> coordAxes = Collections.singletonList(makeAxis("time", AxisType.Time));

    Coverage coverage1 =
        new Coverage("name1", DataType.INT, attributes, "myCoordinateSystem1", "units", "description", null, null);
    List<Coverage> coverages = Collections.singletonList(coverage1);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new CoverageCollection("name",
        FeatureType.GRID, null, null, null, null, coordSys, null, coordAxes, coverages, null));
    assertThat(e).hasMessageThat().contains("must have horiz coordinates (x,y,projection or lat,lon)");
  }

  @Test
  public void shouldRefuseMultipleHorizontalCoordinateSystems() {
    List<CoverageCoordSys> differentLongitudeCoordSystems = Arrays.asList(
        new CoverageCoordSys("myCoordinateSystem1", Arrays.asList("lat1", "lon1"), null, FeatureType.GRID),
        new CoverageCoordSys("myCoordinateSystem2", Arrays.asList("lat1", "lon2"), null, FeatureType.GRID));

    List<CoverageCoordAxis> coordAxes = Arrays.asList(makeAxis("lat1", AxisType.Lat), makeAxis("lon1", AxisType.Lon),
        makeAxis("lat2", AxisType.Lat), makeAxis("lon2", AxisType.Lon));

    Coverage coverage1 =
        new Coverage("name1", DataType.INT, attributes, "myCoordinateSystem1", "units", "description", null, null);
    Coverage coverage2 =
        new Coverage("name2", DataType.INT, attributes, "myCoordinateSystem2", "units", "description", null, null);
    List<Coverage> coverages = Arrays.asList(coverage1, coverage2);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new CoverageCollection("name",
        FeatureType.GRID, null, null, null, null, differentLongitudeCoordSystems, null, coordAxes, coverages, null));
    assertThat(e).hasMessageThat()
        .contains("Cannot open as a coverage collection because there are multiple horizontal coordinate systems");
  }

  @Test
  public void shouldRefuseMultipleDisjointHorizontalCoordinateSystems() {
    List<CoverageCoordSys> disjointCoordSystems = Arrays.asList(
        new CoverageCoordSys("myCoordinateSystem1", Arrays.asList("lat1", "lon1"), null, FeatureType.GRID),
        new CoverageCoordSys("myCoordinateSystem2", Arrays.asList("lat2", "lon2"), null, FeatureType.GRID));

    List<CoverageCoordAxis> coordAxes = Arrays.asList(makeAxis("lat1", AxisType.Lat), makeAxis("lon1", AxisType.Lon),
        makeAxis("lat2", AxisType.Lat), makeAxis("lon2", AxisType.Lon));

    Coverage coverage1 =
        new Coverage("name1", DataType.INT, attributes, "myCoordinateSystem1", "units", "description", null, null);
    Coverage coverage2 =
        new Coverage("name2", DataType.INT, attributes, "myCoordinateSystem2", "units", "description", null, null);
    List<Coverage> coverages = Arrays.asList(coverage1, coverage2);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new CoverageCollection("name",
        FeatureType.GRID, null, null, null, null, disjointCoordSystems, null, coordAxes, coverages, null));
    assertThat(e).hasMessageThat()
        .contains("Cannot open as a coverage collection because there are multiple horizontal coordinate systems");
  }

  private static CoverageCoordAxis makeAxis(String name, AxisType type) {
    final double[] values = new double[] {0.0, 10.0, 20.0, 30.0, 40.0};
    final CoverageCoordAxisBuilder coverageCoordAxisBuilder = new CoverageCoordAxisBuilder(name, "unit", "description",
        DataType.DOUBLE, type, null, CoverageCoordAxis.DependenceType.independent, null, Spacing.regularPoint,
        values.length, values[0], values[values.length - 1], values[1] - values[0], values, null);
    return new CoverageCoordAxis1D(coverageCoordAxisBuilder);
  }
}
