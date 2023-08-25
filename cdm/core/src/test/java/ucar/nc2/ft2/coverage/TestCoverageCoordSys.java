package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCoordAxis.Spacing;
import ucar.nc2.util.Optional;

public class TestCoverageCoordSys {

  @Test
  public void shouldFindAxisCaseSensitive() {
    final CoverageCoordSys coverageCoordSys = createCoverageCoordSys();
    assertThat(coverageCoordSys.getAxis("axis").getName()).isEqualTo("axis");
    assertThat(coverageCoordSys.getAxis("AXIS").getName()).isEqualTo("AXIS");
  }

  @Test
  public void shouldFindAxisCaseSensitiveWhenSubsetting() {
    final CoverageCoordSys coverageCoordSys = createCoverageCoordSys();
    final SubsetParams params = new SubsetParams();
    final Optional<CoverageCoordSys> subset = coverageCoordSys.subset(params);

    assertThat(subset.isPresent()).isTrue();
    assertThat(subset.get().getAxis("axis").getName()).isEqualTo("axis");
    assertThat(subset.get().getAxis("AXIS").getName()).isEqualTo("AXIS");
  }

  private static CoverageCoordAxis1D createCoverageCoordAxis1D(String name, AxisType type) {
    final CoverageCoordAxisBuilder coordAxisBuilder = new CoverageCoordAxisBuilder(name, "days since 2000-01-01",
        "description", DataType.DOUBLE, type, null, CoverageCoordAxis.DependenceType.independent, null,
        Spacing.regularPoint, 3, 0.0, 2.0, 1.0, new double[] {0.0, 1.0, 2.0}, null);
    return new CoverageCoordAxis1D(coordAxisBuilder);
  }

  private static CoverageCoordSys createCoverageCoordSys() {
    final AttributeContainerMutable attributes = new AttributeContainerMutable("attributes");
    final CoverageCoordAxis1D lat = createCoverageCoordAxis1D("lat", AxisType.Lat);
    final CoverageCoordAxis1D lon = createCoverageCoordAxis1D("lon", AxisType.Lon);
    final CoverageCoordAxis1D axis1 = createCoverageCoordAxis1D("axis", AxisType.Time);
    final CoverageCoordAxis1D axis2 = createCoverageCoordAxis1D("AXIS", AxisType.Ensemble);
    final List<CoverageCoordAxis> coordAxes = Arrays.asList(lat, lon, axis1, axis2);
    final List<String> axisNames = coordAxes.stream().map(CoverageCoordAxis::getName).collect(Collectors.toList());

    final CoverageTransform transform = new CoverageTransform("transform1", attributes, true);
    final List<CoverageTransform> coordTransforms = Collections.singletonList(transform);
    final List<String> transformNames = Collections.singletonList(transform.getName());

    final CoverageCoordSys coverageCoordSys =
        new CoverageCoordSys("CoverageCoordSysName", axisNames, transformNames, FeatureType.ANY);
    final List<Coverage> coverages = Collections.singletonList(new Coverage("coverageName", DataType.DOUBLE, attributes,
        coverageCoordSys.getName(), "units", "description", null, null));
    // Creating this CoverageCollection apparently alters the state of the coverageCoordSys
    final CoverageCollection coverageCollection = new CoverageCollection("collectionName", FeatureType.ANY, null, null,
        null, null, Collections.singletonList(coverageCoordSys), coordTransforms, coordAxes, coverages, null);

    return coverageCoordSys;
  }
}
