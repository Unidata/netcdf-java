/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.internal.grid.CurvilinearCoords.CoordReturn;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.Random;

import static com.google.common.truth.Truth.assertThat;

/** Unit test for {@link CurvilinearCoords} */
public class TestCurvilinearCoords {
  private Array<Number> lat2d;
  private Array<Number> lon2d;

  @Before
  public void setup() {
    make(10, 10, (y, x) -> 1.01 * (y + x), (y, x) -> 1.02 * (y + x));
  }

  interface Fn {
    double compute(int y, int x);
  }

  void make(int ny, int nx, Fn lat, Fn lon) {
    int[] shape = new int[] {ny, nx};
    double[] latdata = new double[ny * nx];
    double[] londata = new double[ny * nx];
    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        latdata[y * nx + x] = lat.compute(y, x);
        londata[y * nx + x] = lon.compute(y, x);
      }
    }
    lat2d = Arrays.factory(ArrayType.DOUBLE, shape, latdata);
    lon2d = Arrays.factory(ArrayType.DOUBLE, shape, londata);
  }

  @Test
  public void testEdges() {
    make(10, 10, (y, x) -> y, (y, x) -> x);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    CurvilinearCoords cc = new CurvilinearCoords("test edges", lat2d, lon2d);
    cc.showEdges();
    cc.check();
    assertThat(cc.findIndexFromLatLon(0, 0).orElseThrow()).isEqualTo(new CoordReturn(0, 0, 0, 0));
    assertThat(cc.findIndexFromLatLon(4, 5).orElseThrow()).isEqualTo(new CoordReturn(4, 5, 4, 5));
    assertThat(cc.findIndexFromLatLon(4.8, 5.8).orElseThrow()).isEqualTo(new CoordReturn(4.8, 5.8, 5, 6));

    testRandomValues(cc, 50);

    CurvilinearCoords.MinMaxIndices result = cc.subsetProjectionRect(new ProjectionRect(-10, -10, 12, 13));
    System.out.printf("MinMaxIndices = %s%n", result);
    assertThat(result.isEmpty()).isFalse();
    assertThat(result).isEqualTo(cc.create(0, 0, 9, 9));

    assertThat(cc.showBox(0, 0)).contains(
        "lat1=[-0.500000, -0.500000] lat2=[0.500000, 0.500000] lon1=[-0.500000, 0.500000] lon2=[-0.500000, 0.500000]");
  }

  @Test
  public void testEdgesOffset() {
    double xoff = 100;
    double yoff = 200;
    make(10, 10, (y, x) -> yoff + y, (y, x) -> xoff + x);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    CurvilinearCoords hcs = new CurvilinearCoords("test edges", lat2d, lon2d);
    hcs.showEdges();
    hcs.check();
    assertThat(hcs.findIndexFromLatLon(yoff + 0, xoff + 0).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 0, xoff + 0, 0, 0));
    assertThat(hcs.findIndexFromLatLon(yoff + 4, xoff + 5).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 4, xoff + 5, 4, 5));
    assertThat(hcs.findIndexFromLatLon(yoff + 4.8, xoff + 5.8).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 4.8, xoff + 5.8, 5, 6));

    testRandomValues(hcs, 50);
  }

  @Test
  public void testEdges2() {
    make(10, 10, (y, x) -> 1.1 * y, (y, x) -> 1.2 * x);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    CurvilinearCoords hcs = new CurvilinearCoords("test edges", lat2d, lon2d);
    hcs.showEdges();
    hcs.check();
    assertThat(hcs.findIndexFromLatLon(0, 0).orElseThrow()).isEqualTo(new CoordReturn(0, 0, 0, 0));
    assertThat(hcs.findIndexFromLatLon(4, 5).orElseThrow()).isEqualTo(new CoordReturn(4, 5, 4, 4));
    assertThat(hcs.findIndexFromLatLon(5, 5.8).orElseThrow()).isEqualTo(new CoordReturn(5, 5.8, 5, 5));
    assertThat(hcs.findIndexFromLatLon(8, 10).orElseThrow()).isEqualTo(new CoordReturn(8, 10, 7, 8));

    testRandomValues(hcs, 50);

    CurvilinearCoords.MinMaxIndices result = hcs.subsetProjectionRect(new ProjectionRect(10, -10, 13, 12));
    assertThat(result.isEmpty()).isFalse();
    assertThat(result).isEqualTo(hcs.create(0, 9, 9, 9));
  }

  @Test
  public void testEdges2Offset() {
    double xoff = 100;
    double yoff = 200;
    make(10, 10, (y, x) -> yoff + 1.1 * y, (y, x) -> xoff + 1.2 * x);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    CurvilinearCoords hcs = new CurvilinearCoords("test edges", lat2d, lon2d);
    hcs.showEdges();
    hcs.check();
    assertThat(hcs.findIndexFromLatLon(yoff + 0, xoff + 0).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 0, xoff + 0, 0, 0));
    assertThat(hcs.findIndexFromLatLon(yoff + 4, xoff + 5).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 4, xoff + 5, 4, 4));
    assertThat(hcs.findIndexFromLatLon(yoff + 5, xoff + 5.8).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 5, xoff + 5.8, 5, 5));
    assertThat(hcs.findIndexFromLatLon(yoff + 8, xoff + 10).orElseThrow())
        .isEqualTo(new CoordReturn(yoff + 8, xoff + 10, 7, 8));

    testRandomValues(hcs, 50);
  }

  @Test
  public void testEdges3() {
    make(10, 10, (y, x) -> 1.1 * y + 1.02 * x, (y, x) -> 1.2 * x + 1.01 * y);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    CurvilinearCoords hcs = new CurvilinearCoords("test edges", lat2d, lon2d);
    hcs.showEdges();
    hcs.check();
    assertThat(hcs.findIndexFromLatLon(0, 0).orElseThrow()).isEqualTo(new CoordReturn(0, 0, 0, 0));
    assertThat(hcs.findIndexFromLatLon(5, 6).orElseThrow()).isEqualTo(new CoordReturn(5, 6, 0, 5));
    assertThat(hcs.findIndexFromLatLon(11, 11).orElseThrow()).isEqualTo(new CoordReturn(11, 11, 7, 3));

    // failures
    assertThat(hcs.findIndexFromLatLon(4, 5).isEmpty());

    testRandomValues(hcs, 50);

    CurvilinearCoords.MinMaxIndices result = hcs.subsetProjectionRect(new ProjectionRect(-3, 3, 10, 4));
    assertThat(result).isEqualTo(hcs.create(0, 0, 3, 3)); // no intersection
  }

  @Test
  public void testColinearPoints() {
    make(10, 10, (y, x) -> 1.01 * (y + x), (y, x) -> 1.02 * (y + x));
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test2 lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test2 lon2d", null));
    CurvilinearCoords hcs = new CurvilinearCoords("test2 edges", lat2d, lon2d);
    hcs.showEdges();
    hcs.check();

    // failures
    assertThat(hcs.findIndexFromLatLon(0, 0).isEmpty());
    assertThat(hcs.findIndexFromLatLon(10, 10).isEmpty());
  }

  private static final Random random = new Random();

  private void testRandomValues(CurvilinearCoords cc, int repeat) {
    int nrows = lat2d.getShape()[0] - 1;
    int ncols = lat2d.getShape()[1] - 1;

    for (int count = 0; count < repeat; count++) {
      int latidx = random.nextInt(nrows);
      int lonidx = random.nextInt(ncols);

      CoordReturn mid = cc.midpoint(latidx, lonidx);
      // System.out.printf("%d [%f, %f]%n", count, mid.lat, mid.lon);
      assertThat(cc.findIndexFromLatLon(mid.lat, mid.lon).orElseThrow())
          .isEqualTo(new CoordReturn(mid.lat, mid.lon, latidx, lonidx));
    }
  }

}
