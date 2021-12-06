/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test permuted example file. */
public class TestReadPermute {

  @Test
  public void testReadPermute() throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset dataset =
        GridDatasetFactory.openGridDataset(TestDir.cdmLocalTestDataDir + "permuteTest.nc", errlog)) {

      doRead4(dataset, "tzyx");
      doRead4(dataset, "tzxy");
      doRead4(dataset, "txyz");
      doRead4(dataset, "tyxz");
      doRead4(dataset, "zyxt");
      doRead4(dataset, "zxyt");
      doRead4(dataset, "xyzt");
      doRead4(dataset, "yxzt");

      doRead3(dataset, "zyx");
      doRead3(dataset, "txy");
      doRead3(dataset, "yxz");
      doRead3(dataset, "xzy");
      doRead3(dataset, "yxt");
      doRead3(dataset, "xyt");
      doRead3(dataset, "yxt");
      doRead3(dataset, "xyz");

      doRead2(dataset, "yx");
      doRead2(dataset, "xy");
      // not grids
      // doRead2(dataset, "yz");
      // doRead2(dataset, "xz");
      // doRead2(dataset, "yt");
      // doRead2(dataset, "xt");
      // doRead2(dataset, "ty");
      // doRead2(dataset, "tx");
    }

    System.out.println("*****************Test Read done");
  }

  private void doRead4(GridDataset ds, String varName) throws IOException, InvalidRangeException {
    Grid gg = ds.findGrid(varName).orElseThrow();

    GridReferencedArray gra = gg.readData(GridSubset.create());
    Array<Number> aa = gra.data();
    int[] shape = aa.getShape();
    int[] w = getWeights(gg);
    Index index = aa.getIndex();

    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          for (int m = 0; m < shape[3]; m++) {
            double got = aa.get(index.set(i, j, k, m)).doubleValue();
            double want = i * w[0] + j * w[1] + k * w[2] + m * w[3];
            assertThat(got).isEqualTo(want);
            // System.out.println("got "+got+ " want "+want);
          }
        }
      }
    }

    System.out.println("ok reading " + varName);
  }

  private void doRead3(GridDataset ds, String varName) throws IOException, InvalidRangeException {
    Grid gg = ds.findGrid(varName).orElseThrow();

    GridReferencedArray gra = gg.readData(GridSubset.create());
    Array<Number> aa = gra.data();
    int[] shape = aa.getShape();
    int[] w = getWeights(gg);
    Index index = aa.getIndex();

    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        for (int k = 0; k < shape[2]; k++) {
          double got = aa.get(index.set(i, j, k)).doubleValue();
          double want = i * w[0] + j * w[1] + k * w[2];
          assertThat(got).isEqualTo(want);
        }
      }
    }

    System.out.println("ok reading " + varName);
  }

  private void doRead2(GridDataset ds, String varName) throws IOException, InvalidRangeException {
    Grid gg = ds.findGrid(varName).orElseThrow(() -> new IllegalStateException(varName));

    GridReferencedArray gra = gg.readData(GridSubset.create());
    Array<Number> aa = gra.data();
    int[] shape = aa.getShape();
    int[] w = getWeights(gg);
    Index index = aa.getIndex();

    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double got = aa.get(index.set(i, j)).doubleValue();
        double want = i * w[0] + j * w[1];
        assertThat(got).isEqualTo(want);
      }
    }

    System.out.println("ok reading " + varName);
  }

  private int[] getWeights(Grid gg) {
    int rank = gg.getCoordinateSystem().getNominalShape().size();
    int[] w = new int[rank];

    for (int n = 0; n < rank; n++) {
      char c = gg.getName().charAt(n);
      switch (c) {
        case 't':
          w[n] = 1000;
          break;
        case 'z':
          w[n] = 100;
          break;
        case 'y':
          w[n] = 10;
          break;
        case 'x':
          w[n] = 1;
          break;
        default:
          throw new IllegalStateException(gg.getName());
      }
    }
    return w;
  }
}
