/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.Format;

import java.lang.invoke.MethodHandles;
import java.util.Random;

import static com.google.common.truth.Truth.assertThat;

/** Test basic projection methods */
public class TestLatLonRect {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final boolean debug2 = false;
  private final boolean debug3 = false;
  private final boolean debug4 = false;

  /////////////////// testLatLonArea /////////////////

  LatLonRect makeLatLonBoundingBoxPtsOld(double lon1, double lon2) {
    LatLonPoint pt1 = LatLonPoint.create(-10.0, lon1);
    LatLonPoint pt2 = LatLonPoint.create(10.0, lon2);
    LatLonRect llbb = new LatLonRect.Builder(pt1, pt2).build();
    if (debug2)
      System.out.println(Format.formatDouble(lon1, 8, 5) + " " + Format.formatDouble(lon2, 8, 5) + " => " + llbb
              + " crossDateline= " + llbb.crossDateline());
    return llbb;
  }

  LatLonRect makeLatLonBoundingBoxPts(double lon1, double lon2) {
    LatLonPoint pt = LatLonPoint.create(-10.0, lon1);
    LatLonRect llbb = new LatLonRect(pt, 20.0, lon2 - lon1);
    if (debug2)
      System.out.println(Format.formatDouble(lon1, 8, 5) + " " + Format.formatDouble(lon2, 8, 5) + " => " + llbb
              + " crossDateline= " + llbb.crossDateline());
    return llbb;
  }

  LatLonRect makeLatLonBoundingBoxInc(double lon, double loninc) {
    LatLonPoint pt = LatLonPoint.create(-10.0, lon);
    LatLonRect llbb = new LatLonRect(pt, 20.0, loninc);
    if (debug2)
      System.out.println(Format.formatDouble(lon, 8, 5) + " " + Format.formatDouble(loninc, 8, 5) + " => " + llbb
          + " crossDateline= " + llbb.crossDateline());
    return llbb;
  }

  void testContains(LatLonRect b1, LatLonRect b2) {
    if (debug3)
      System.out.println(b1 + " crossDateline= " + b1.crossDateline());
    if (debug3)
      System.out.println(b2 + " crossDateline= " + b2.crossDateline());
    if (debug3)
      b1.containedIn(b2);
    else
      assertThat (b1.containedIn(b2));
  }

  LatLonRect testExtend(LatLonRect b, LatLonPoint pt) {
    if (debug4)
      System.out.println("start " + b + " crossDateline= " + b.crossDateline());
    LatLonRect bextend = b.toBuilder().extend(pt).build();
    if (debug4)
      System.out.println("extend " + pt + " ==> " + b + " crossDateline= " + bextend.crossDateline());
    if (!debug4)
      assertThat (bextend.contains(pt));
    return bextend;
  }

  @Test
  public void testGlobalBB() {
    LatLonRect llbb = new LatLonRect();
    assertThat(llbb.crossDateline()).isFalse();
    assertThat(llbb.isAllLongitude()).isTrue();
    assertThat(llbb.getWidth()).isEqualTo(360);
    assertThat(llbb.getHeight()).isEqualTo(180);
    assertThat(llbb.getCenterLon()).isEqualTo(0);

    assertThat(llbb.getLonMin()).isEqualTo(-180);
    assertThat(llbb.getLonMax()).isEqualTo(180);
    assertThat(llbb.getLatMin()).isEqualTo(-90);
    assertThat(llbb.getLatMax()).isEqualTo(90);

    assertThat(llbb.getUpperLeftPoint()).isEqualTo(LatLonPoint.create(90, -180));
    assertThat(llbb.getLowerLeftPoint()).isEqualTo(LatLonPoint.create(-90, -180));
    assertThat(llbb.getUpperRightPoint()).isEqualTo(LatLonPoint.create(90, 180));
    assertThat(llbb.getLowerRightPoint()).isEqualTo(LatLonPoint.create(-90, 180));
  }

  @Test
  public void testUnormalized() {
    LatLonRect llbb = new LatLonRect(11, 120, 13, 900);
    assertThat(llbb.crossDateline()).isFalse();
    assertThat(llbb.isAllLongitude()).isFalse();
    assertThat(llbb.getWidth()).isEqualTo(60);
    assertThat(llbb.getHeight()).isEqualTo(2);
    assertThat(llbb.getCenterLon()).isEqualTo(150);

    assertThat(llbb.getLonMin()).isEqualTo(120);
    assertThat(llbb.getLonMax()).isEqualTo(180);
    assertThat(llbb.getLatMin()).isEqualTo(11);
    assertThat(llbb.getLatMax()).isEqualTo(13);
  }

  @Test
  public void testGlobalBBcontains() {
    Random rand = new Random(System.currentTimeMillis());
    int count = 0;
    while (count++ < 1000) {
      double r = 360. * rand.nextFloat() - 180;
      LatLonRect llbb = new LatLonRect(LatLonPoint.create(20.0, r), 20.0, 360.0);
      double r2 = 360. * rand.nextFloat() - 180;
      LatLonPoint p = LatLonPoint.create(30.0, r2);
      assertThat(llbb.contains(p)).isTrue();
    }
  }

  @Test
  public void testSpec() {
    LatLonRect rect1 = LatLonRect.builder(LatLonPoint.create(99, 100), 101, 102).build();
    LatLonRect rect2 = LatLonRect.builder("99, 100, 101, 102").build();
    assertThat (rect1).isEqualTo(rect2);
  }

  @Test
  public void testLatLonBoundingBox() {
    /* check constructors */
    assertThat(makeLatLonBoundingBoxInc(140.0, 50.0)).isEqualTo(makeLatLonBoundingBoxPts(140.0, 190.0));
    assertThat(makeLatLonBoundingBoxInc(-170.0, 310.0)).isEqualTo(makeLatLonBoundingBoxPts(190.0, 140.0));
    assertThat(makeLatLonBoundingBoxInc(190.0, 310.0)).isEqualTo(makeLatLonBoundingBoxPts(190.0, 140.0));
  }

  @Test
  public void problemCross() {
    LatLonRect rectPts = makeLatLonBoundingBoxInc(0.0, -250.0);
    assertThat(rectPts.crossDateline()).isFalse();
  }

  @Test
  public void testCrossDateline() {
    // check dateline crossings
    assertThat(makeLatLonBoundingBoxInc(140.0, 50.0).crossDateline()).isTrue();
    assertThat(makeLatLonBoundingBoxInc(140.0, -150.0).crossDateline()).isTrue();
    assertThat(makeLatLonBoundingBoxInc(0.0, -250.0).crossDateline()).isFalse();
    assertThat(makeLatLonBoundingBoxInc(-170.0, 300.0).crossDateline()).isFalse();
    assertThat(makeLatLonBoundingBoxPts(140.0, 190.0).crossDateline()).isTrue();
    assertThat(makeLatLonBoundingBoxPts(190.0, 140.0).crossDateline()).isFalse();
    assertThat(makeLatLonBoundingBoxInc(171.0, 370.0).crossDateline()).isTrue();
  }

  @Test
  public void testContains() {
    // contains point
    LatLonPoint pt = LatLonPoint.create(0.0, 177.0);
    assertThat(makeLatLonBoundingBoxInc(140.0, 50.0).contains(pt)).isTrue();
    assertThat(!makeLatLonBoundingBoxInc(190.0, 310.0).contains(pt)).isTrue();
    assertThat(makeLatLonBoundingBoxPts(140.0, 190.0).contains(pt)).isTrue();
    assertThat(!makeLatLonBoundingBoxInc(190.0, 140.0).contains(pt)).isTrue();

    // contained in
    testContains(makeLatLonBoundingBoxInc(140.0, 50.0), makeLatLonBoundingBoxInc(140.0, 50.0));
    testContains(makeLatLonBoundingBoxPts(140.0, 50.0), makeLatLonBoundingBoxPts(140.0, 50.0));
    testContains(makeLatLonBoundingBoxInc(140.0, 50.0), makeLatLonBoundingBoxInc(0, 360.0));
    testContains(makeLatLonBoundingBoxInc(300.0, 50.0), makeLatLonBoundingBoxInc(0, 360.0));
    testContains(makeLatLonBoundingBoxInc(50.0, 300.0), makeLatLonBoundingBoxInc(0, 360.0));
    testContains(makeLatLonBoundingBoxInc(50.0, 300.0), makeLatLonBoundingBoxInc(-180.0, 360.0));
    testContains(makeLatLonBoundingBoxPts(190.0, 10.0), makeLatLonBoundingBoxPts(140.0, 50.0));
    testContains(makeLatLonBoundingBoxInc(190.0, 10.0), makeLatLonBoundingBoxInc(140.0, 60.0));
  }

  @Test
  public void testExtends() {
    // extend
    LatLonRect b;
    LatLonPoint p = LatLonPoint.create(30.0, 30.0);
    b = testExtend(makeLatLonBoundingBoxInc(10.0, 10.0), p);
    assertThat (p.nearlyEquals(b.getUpperRightPoint()));

    p = LatLonPoint.create(-30.0, -30.0);
    b = testExtend(makeLatLonBoundingBoxInc(10.0, 10.0), p);
    assertThat (p.nearlyEquals(b.getLowerLeftPoint()));

    p = LatLonPoint.create(30.0, 190.0);
    b = testExtend(makeLatLonBoundingBoxInc(50.0, 100.0), p);
    assertThat (p.nearlyEquals(b.getUpperRightPoint()));
    assertThat (b.crossDateline());

    p = LatLonPoint.create(-30.0, -50.0);
    b = testExtend(makeLatLonBoundingBoxInc(50.0, 100.0), p);
    assertThat (p.nearlyEquals(b.getLowerLeftPoint()));
    assertThat (!b.crossDateline());

    p = LatLonPoint.create(-30.0, 100.0);
    b = testExtend(makeLatLonBoundingBoxPts(140.0, 50.0), p);
    assertThat (p.nearlyEquals(b.getLowerLeftPoint()));
    assertThat (b.crossDateline());

    p = LatLonPoint.create(30.0, 55.0);
    b = testExtend(makeLatLonBoundingBoxPts(140.0, 50.0), p);
    assertThat (p.nearlyEquals(b.getUpperRightPoint()));
    assertThat (b.crossDateline());
  }

}
