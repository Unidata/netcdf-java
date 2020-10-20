/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ucar.unidata.geoloc.projection.LatLonProjection} */
public class TestLatLonProjection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private LatLonProjection p;

  @Before
  public void setUp() {
    p = new LatLonProjection();
  }

  /*
   * void showLatLonNormal(double lon, double center) {
   * System.out.println( Format.formatDouble(lon, 8, 5)+ " => "+
   * Format.formatDouble(LatLonPoint.lonNormal( lon, center), 8, 5));
   * }
   */

  void runCenter() {
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      LatLonPoint ptL = LatLonPoint.create(-73.79, lon);
      LatLonRect llbb = LatLonRect.builder(ptL, yinc, xinc).build();

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      Assert.assertTrue(llbb + " => " + ma2 + " => " + p2, llbb.nearlyEquals(p2));
    }
  }

  void runCenter(double center) {
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      LatLonPoint ptL = LatLonPoint.create(0, center + lon);
      LatLonRect llbb = LatLonRect.builder(ptL, yinc, xinc).build();

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      Assert.assertTrue(llbb + " => " + ma2 + " => " + p2, llbb.nearlyEquals(p2));
    }
  }

  @Test
  public void testLatLonToProjBB() {
    runCenter();
    runCenter(110.45454545454547);
    runCenter(-110.45454545454547);
    runCenter(0.0);
    runCenter(420.0);
  }

  public LatLonRect testIntersection(LatLonRect bbox, LatLonRect bbox2) {
    LatLonRect result = bbox.intersect(bbox2);
    if (result != null)
      Assert.assertEquals("bbox= " + bbox.toString2() + "\nbbox2= " + bbox2.toString2() + "\nintersect= "
          + (result == null ? "null" : result.toString2()), bbox.intersect(bbox2), bbox2.intersect(bbox));
    return result;
  }

  @Test
  public void testIntersection() {
    LatLonRect bbox = LatLonRect.builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
    LatLonRect bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 300.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox2 = LatLonRect.builder(LatLonPoint.create(10, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) == null;

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, 120.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -220.0), 120.0, 140.0).build();
    assert testIntersection(bbox, bbox2) != null;
  }

  private LatLonRect testExtend(LatLonRect.Builder bbox, LatLonRect bbox2) {
    bbox.extend(bbox2);
    return bbox.build();
  }

  @Test
  public void testExtend() {
    LatLonRect bbox;

    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, 30.0), LatLonPoint.create(-60.0, 120.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -10.0), LatLonPoint.create(-60.0, 55.0)).build());
    Assert.assertEquals(bbox.toString2(), 130.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 177.0), LatLonPoint.create(-60.0, 200.0)).build());
    Assert.assertEquals(bbox.toString2(), 100.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // --------------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -150.0), LatLonPoint.create(-60.0, 200.0)).build());
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    // -------
    // ---------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 135.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    // ------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 160.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());
  }

  @Test
  public void testLatLonToProjRect() {
    ProjectionRect[] result = p.latLonToProjRect( 0, 20, 40, 60);
    assertThat(result).asList().containsExactly(new ProjectionRect(20, 0, 60, 40), null);

    result = p.latLonToProjRect ( 0, 100, 40, 260);
    assertThat(result).asList().containsExactly(new ProjectionRect(100, 0, 180, 40),
            new ProjectionRect(-180, 0, -100, 40));

    result = p.latLonToProjRect ( 0, 100, 40, -100);
    assertThat(result).asList().containsExactly(new ProjectionRect(100, 0, 180, 40),
            new ProjectionRect(-180, 0, -100, 40));

    result = p.latLonToProjRect ( 0, 0, 40, 360);
    assertThat(result).asList().containsExactly(new ProjectionRect(0, 0, 180, 40),
            new ProjectionRect(-180, 0, 0, 40));

    result = p.latLonToProjRect ( 0, 0, 40, 1.e-9);
    assertThat(result).asList().containsExactly(new ProjectionRect(0, 0, 180, 40),
            new ProjectionRect(-180, 0, 0, 40));

    result = p.latLonToProjRect ( 0, -180, 40, -180);
    assertThat(result).asList().containsExactly(new ProjectionRect(-180, 0, 180, 40), null);

    LatLonProjection p2 = new LatLonProjection("center180", null, 180);
    ProjectionRect[] result2 = p2.latLonToProjRect ( LatLonRect.builder("0, -10, 99, 20").build());
    assertThat(result2).asList().containsExactly(
            ProjectionRect.builder().setRect(350, 0, 10, 90).build(),
            ProjectionRect.builder().setRect(0, 0, 10, 90).build());

    result2 = p2.latLonToProjRect ( LatLonRect.builder("0, 111, 99, 200").build());
    assertThat(result2).asList().containsExactly(
            ProjectionRect.builder().setRect(111, 0, 200, 90).build(),
            null);
  }

  @Test
  public void problem2() {
    ProjectionRect[] result = p.latLonToProjRect ( 0, -180, 40, -180);
    assertThat(result).asList().containsExactly(new ProjectionRect(-180, 0, 180, 40), null);
  }



}
