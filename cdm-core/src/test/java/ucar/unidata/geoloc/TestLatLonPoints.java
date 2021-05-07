/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.unidata.geoloc.LatLonPoints}, {@link ucar.unidata.geoloc.ProjectionPoint} */
public class TestLatLonPoints {

  @Test
  public void testBetweenLon() {
    assertThat(LatLonPoints.betweenLon(90, 89, 91)).isTrue();
    assertThat(LatLonPoints.betweenLon(90, 89, 91 - 360)).isTrue();
    assertThat(LatLonPoints.betweenLon(90 - 360, 89, 91)).isTrue();

    assertThat(LatLonPoints.betweenLon(90, 91, -180)).isFalse();
    assertThat(LatLonPoints.betweenLon(90, 91, -360)).isFalse();
    assertThat(LatLonPoints.betweenLon(90 - 360, 91, 89)).isFalse();

    assertThat(LatLonPoints.betweenLon(180, 180, -180)).isTrue();
    assertThat(LatLonPoints.betweenLon(-180, 180, -180)).isTrue();
    assertThat(LatLonPoints.betweenLon(-179, 180, -180)).isFalse();
    assertThat(LatLonPoints.betweenLon(-181, 180, -180)).isFalse();

    assertThat(LatLonPoints.betweenLon(-179, 180, -178)).isTrue();
    assertThat(LatLonPoints.betweenLon(-181, 180, -178)).isFalse();
  }

  @Test
  public void testLonDiff() {
    assertThat(LatLonPoints.lonDiff(90, 89)).isEqualTo(1);
    assertThat(LatLonPoints.lonDiff(90, 99)).isEqualTo(-9);

    assertThat(LatLonPoints.lonDiff(90 - 360, 89 + 360)).isEqualTo(1);
    assertThat(LatLonPoints.lonDiff(90 - 360, 99 + 360)).isEqualTo(-9);
  }

  @Test
  public void testLatNormal() {
    assertThat(LatLonPoints.latNormal(89.5)).isEqualTo(89.5);
    assertThat(LatLonPoints.latNormal(92)).isEqualTo(90);
    assertThat(LatLonPoints.latNormal(-99)).isEqualTo(-90);
  }

  @Test
  public void testLatToString() {
    assertThat(LatLonPoints.latToString(89.501, 3)).isEqualTo("89.501N");
    assertThat(LatLonPoints.latToString(89.501, 2)).isEqualTo("89.50N");
    assertThat(LatLonPoints.latToString(89.501, 1)).isEqualTo("89.5N");
    assertThat(LatLonPoints.latToString(89.401, 0)).isEqualTo("89N");

    assertThat(LatLonPoints.latToString(99.999, 0)).isEqualTo("100N");
    assertThat(LatLonPoints.latToString(99.999, 1)).isEqualTo("100.0N");
    assertThat(LatLonPoints.latToString(99.999, 2)).isEqualTo("100.00N");
    assertThat(LatLonPoints.latToString(99.999, 3)).isEqualTo("99.999N");
  }

  @Test
  public void testLonToString() {
    assertThat(LatLonPoints.lonToString(89.501, 3)).isEqualTo("89.501E");
    assertThat(LatLonPoints.lonToString(89.501, 2)).isEqualTo("89.50E");
    assertThat(LatLonPoints.lonToString(89.501, 1)).isEqualTo("89.5E");
    assertThat(LatLonPoints.lonToString(89.401, 0)).isEqualTo("89E");

    assertThat(LatLonPoints.lonToString(-199.999, 0)).isEqualTo("160E");
    assertThat(LatLonPoints.lonToString(399.999, 1)).isEqualTo("40.0E");
    assertThat(LatLonPoints.lonToString(399.999, 2)).isEqualTo("40.00E");
    assertThat(LatLonPoints.lonToString(399.999, 3)).isEqualTo("39.999E");
  }

  @Test
  public void testLatLonToString() {
    assertThat(LatLonPoints.toString(LatLonPoint.create(89.501, 399.999), 3)).isEqualTo("89.5, 39.9");
  }

  @Test
  public void testProjPointToString() {
    assertThat(LatLonPoints.toString(ProjectionPoint.create(89.501, 399.999), 3)).isEqualTo("89.5, 399");
  }

  @Test
  public void testIsInfinite() {
    assertThat(LatLonPoints.isInfinite(ProjectionPoint.create(89.501, 399.999))).isFalse();
    assertThat(LatLonPoints.isInfinite(ProjectionPoint.create(89.501, Double.NaN))).isFalse(); // LOOK ok?
    assertThat(LatLonPoints.isInfinite(ProjectionPoint.create(89.501, Double.POSITIVE_INFINITY))).isTrue();
  }

  @Test
  public void testProjectionPointEqual() {
    ProjectionPoint pt1 = ProjectionPoint.create(89.501, 399.999);
    ProjectionPoint pt2 = ProjectionPoint.create(89.501, 399.99);
    assertThat(pt1.nearlyEquals(pt2)).isFalse();
    assertThat(pt1.nearlyEquals(pt2, 1.0e-3)).isTrue();
  }

}
