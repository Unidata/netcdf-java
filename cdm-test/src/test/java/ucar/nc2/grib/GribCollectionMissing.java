/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridReader;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridTimeCoordinateSystem;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Count missing data in Grib Collections.
 */
public class GribCollectionMissing {

  static public class Count {
    int nread;
    int nmiss;
    int nerrs;

    void add(Count c) {
      nread += c.nread;
      nmiss += c.nmiss;
      nerrs += c.nerrs;
    }
  }

  ///////////////////////////////////////////////////////////////

  public static Count read(String filename) {
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    Count allCount = new Count();
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      for (Grid gdt : gds.getGrids()) {
        Count gridCount = new Count();
        readAll(gdt, gridCount);
        System.out.printf("%80s == %d/%d (%d)%n", gdt.getName(), gridCount.nmiss, gridCount.nread, gridCount.nerrs);
        allCount.add(gridCount);
      }
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / allCount.nread;
      System.out.printf("%n%80s == %d/%d (%d)%n", "total", allCount.nmiss, allCount.nread, allCount.nerrs);
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took / 1000, r);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      Formatter out = new Formatter(System.out);
      GribCdmIndex.gribCollectionCache.showCache(out);
    }

    return allCount;
  }

  public static void readAll(Grid grid, Count count) {
    GridCoordinateSystem gcs = grid.getCoordinateSystem();
    assertThat(gcs).isNotNull();
    GridReader reader = grid.getReader();

    GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
    if (tcs != null) {
      GridAxisPoint runtime = tcs.getRunTimeAxis();
      if (runtime != null) {
        for (int runtimeIdx = 0; runtimeIdx < runtime.getNominalSize(); runtimeIdx++) {
          reader.setRunTime(tcs.getRuntimeDate(runtimeIdx));
          readAllTimes(grid, reader, runtimeIdx, count);
        }
      } else {
        readAllTimes(grid, reader, 0, count);
      }
    } else {
      readAllVert(grid, reader, count);
    }
  }

  private static void readAllTimes(Grid grid, GridReader reader, int runtimeIdx, Count count) {
    GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
    if (tcs != null) {
      GridAxis<?> time = tcs.getTimeOffsetAxis(runtimeIdx);
      for (Object coord : time) {
        reader.setTimeOffsetCoord(coord);
        readAllVert(grid, reader, count);
      }
    }
  }

  private static void readAllVert(Grid grid, GridReader reader, Count count) {
    GridCoordinateSystem gcs = grid.getCoordinateSystem();
    GridAxis<?> vert = gcs.getVerticalAxis();
    if (vert != null) {
      for (Object coord : vert) {
        reader.setVertCoord(coord);
        readHorizSlice(reader, count);
      }
    } else {
      readHorizSlice(reader, count);
    }
  }

  private static void readHorizSlice(GridReader reader, Count count) {
    // subset the array by striding x,y by 100
    reader.setHorizStride(100);
    try {
      GridReferencedArray gra = reader.read();
      Array<Number> data = gra.data();
      if (testMissing(data)) {
        count.nmiss++;
      } else {
        count.nread++;
      }
    } catch (InvalidRangeException | IOException e) {
      count.nerrs++;
    }
  }

  // they all have to be missing values
  private static boolean testMissing(Array<Number> data) {
    for (Number val : data) {
      if (!Float.isNaN(val.floatValue()))
        return false;
    }
    return true;
  }
}
