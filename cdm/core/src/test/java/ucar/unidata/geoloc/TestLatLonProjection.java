/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.projection.*;
import java.lang.invoke.MethodHandles;

/** Test the LatLonProjection */
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
      LatLonRect llbb = new LatLonRect.Builder(ptL, yinc, xinc).build();

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
      LatLonRect llbb = new LatLonRect.Builder(ptL, yinc, xinc).build();

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
    LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
    LatLonRect bbox2 = new LatLonRect.Builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox = new LatLonRect.Builder(LatLonPoint.create(-90.0, -100.0), 90.0, 300.0).build();
    bbox2 = new LatLonRect.Builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox2 = new LatLonRect.Builder(LatLonPoint.create(10, -180.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) == null;

    bbox = new LatLonRect.Builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = new LatLonRect.Builder(LatLonPoint.create(-40.0, 120.0), 120.0, 300.0).build();
    assert testIntersection(bbox, bbox2) != null;

    bbox = new LatLonRect.Builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = new LatLonRect.Builder(LatLonPoint.create(-40.0, -220.0), 120.0, 140.0).build();
    assert testIntersection(bbox, bbox2) != null;
  }

  private LatLonRect testExtend(LatLonRect.Builder bbox, LatLonRect bbox2) {
    bbox.extend(bbox2);
    return bbox.build();
  }

  @Test
  public void testExtend() {
    LatLonRect bbox;

    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, 30.0), LatLonPoint.create(-60.0, 120.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, -10.0), LatLonPoint.create(-60.0, 55.0)).build());
    Assert.assertEquals(bbox.toString2(), 130.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, 177.0), LatLonPoint.create(-60.0, 200.0)).build());
    Assert.assertEquals(bbox.toString2(), 100.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // --------------
    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, -150.0), LatLonPoint.create(-60.0, 200.0)).build());
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    // -------
    // ---------
    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 135.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    Assert.assertEquals(bbox.toString2(), 360.0, bbox.getWidth(), 0.01);
    Assert.assertFalse(bbox.toString2(), bbox.crossDateline());

    // ------
    // ------
    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 160.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // ------
    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());

    // ---------
    // ------
    bbox = testExtend(new LatLonRect.Builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)),
        new LatLonRect.Builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)).build());
    Assert.assertEquals(bbox.toString2(), 225.0, bbox.getWidth(), 0.01);
    Assert.assertTrue(bbox.toString2(), bbox.crossDateline());
  }

}
