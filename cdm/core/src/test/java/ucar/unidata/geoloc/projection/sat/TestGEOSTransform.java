package ucar.unidata.geoloc.projection.sat;

import static com.google.common.truth.Truth.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;

public class TestGEOSTransform {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final double subLonDegrees = -75.0;
  private final boolean isSweepX = true; // GOES
  private final Projection projection = new Geostationary(subLonDegrees, isSweepX);

  private static final double TOLERANCE = 1e-6;

  @Test
  public void shouldConvertProjectionPoint() {
    final double[] xValues = new double[] {-0.1, 0, 0.1};
    final double[] yValues = new double[] {-0.1, 0, 0.1};

    for (double x : xValues) {
      for (double y : yValues) {
        final ProjectionPoint projectionPoint = ProjectionPoint.create(x, y);
        final LatLonPoint computedLatLonPoint = projection.projToLatLon(projectionPoint);
        final ProjectionPoint computedProjectionPoint = projection.latLonToProj(computedLatLonPoint);
        assertThat(computedProjectionPoint.getX()).isWithin(TOLERANCE).of(projectionPoint.getX());
        assertThat(computedProjectionPoint.getY()).isWithin(TOLERANCE).of(projectionPoint.getY());
      }
    }
  }

  @Test
  public void shouldConvertProjectionPointToExpectedLatLon() {
    // Values taken from GOES-R product definition and users guide
    final double x = -0.024052;
    final double y = 0.095340;
    final double expectedLat = 33.846162;
    final double expectedLon = -84.690932;

    final ProjectionPoint projectionPoint = ProjectionPoint.create(x, y);

    final LatLonPoint computedLatLonPoint = projection.projToLatLon(projectionPoint);
    assertThat(computedLatLonPoint.getLatitude()).isWithin(TOLERANCE).of(expectedLat);
    assertThat(computedLatLonPoint.getLongitude()).isWithin(TOLERANCE).of(expectedLon);

    final ProjectionPoint computedProjectionPoint = projection.latLonToProj(computedLatLonPoint);
    assertThat(computedProjectionPoint.getX()).isWithin(TOLERANCE).of(projectionPoint.getX());
    assertThat(computedProjectionPoint.getY()).isWithin(TOLERANCE).of(projectionPoint.getY());
  }

  @Test
  public void shouldReturnNanForNotVisibleProjectionPoints() {
    // Approximately the corners of the box containing full disk
    final List<ProjectionPoint> projectionPoints = Arrays.asList(ProjectionPoint.create(-0.15, -0.15),
        ProjectionPoint.create(-0.15, 0.15), ProjectionPoint.create(0.15, -0.15), ProjectionPoint.create(0.15, 0.15));

    for (ProjectionPoint projectionPoint : projectionPoints) {
      final LatLonPoint computedLatLonPoint = projection.projToLatLon(projectionPoint);
      assertThat(computedLatLonPoint.getLatitude()).isNaN();
      assertThat(computedLatLonPoint.getLongitude()).isNaN();
    }
  }

  @Test
  public void shouldNotReturnNanForVisibleProjectionPoints() {
    // Approximately the edges of the full disk
    final List<ProjectionPoint> projectionPoints = Arrays.asList(ProjectionPoint.create(0, -0.1512),
        ProjectionPoint.create(0, 0.1512), ProjectionPoint.create(-0.1518, 0), ProjectionPoint.create(0.1518, 0));

    for (ProjectionPoint projectionPoint : projectionPoints) {
      final LatLonPoint computedLatLonPoint = projection.projToLatLon(projectionPoint);
      assertThat(computedLatLonPoint.getLatitude()).isNotNaN();
      assertThat(computedLatLonPoint.getLongitude()).isNotNaN();
    }
  }

  @Test
  public void shouldReturnNanForNotVisibleLatLonPoints() {
    final List<LatLonPoint> latLonPoints = Arrays.asList(LatLonPoint.create(50, 5), LatLonPoint.create(-50, 5),
        LatLonPoint.create(50, -160), LatLonPoint.create(-50, -160));

    for (LatLonPoint latLonPoint : latLonPoints) {
      final ProjectionPoint computedProjectionPoint = projection.latLonToProj(latLonPoint);
      assertThat(computedProjectionPoint.getX()).isNaN();
      assertThat(computedProjectionPoint.getY()).isNaN();
    }
  }

  @Test
  public void shouldNotReturnNanForVisibleLatLonPoints() {
    final List<LatLonPoint> latLonPoints = Arrays.asList(LatLonPoint.create(-78, -75), LatLonPoint.create(78, -75),
        LatLonPoint.create(0, -154), LatLonPoint.create(0, 4));

    for (LatLonPoint latLonPoint : latLonPoints) {
      final ProjectionPoint computedProjectionPoint = projection.latLonToProj(latLonPoint);
      assertThat(computedProjectionPoint.getX()).isNotNaN();
      assertThat(computedProjectionPoint.getY()).isNotNaN();
    }
  }
}
