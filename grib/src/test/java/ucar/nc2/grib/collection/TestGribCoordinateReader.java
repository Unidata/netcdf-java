/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

public class TestGribCoordinateReader {
  @Test
  public void fullAndSectionReadsMatchEagerCoordinates() throws IOException, InvalidRangeException {
    for (double[] axis : new double[][] {{0, 1}, {-180, 0.1}, {90, -0.01}, {103.829457843, 0.0000011234567},
        {-0.1, 0.1}, {100000, -123.456789}, {1, 0}}) {
      Variable coordinate = coordinate(8193, axis[0], axis[1]);
      Array eager = Array.makeArray(DataType.FLOAT, 8193, axis[0], axis[1]);
      assertThat(coordinate.hasCachedData()).isFalse();
      Section section = new Section("129:8100:31");
      assertThat(coordinate.read(section).copyTo1DJavaArray())
          .isEqualTo(eager.sectionNoReduce(section.getRanges()).copyTo1DJavaArray());
      assertThat(coordinate.hasCachedData()).isFalse();
      assertThat(coordinate.read().copyTo1DJavaArray()).isEqualTo(eager.copyTo1DJavaArray());
    }
  }

  @Test
  public void returnedDataDoesNotChangeSubsequentReads() throws IOException, InvalidRangeException {
    // Exercise both automatically cached small axes and uncached large axes.
    for (int length : new int[] {8, 8193}) {
      Variable coordinate = coordinate(length, -180, 0.1);
      Array first = coordinate.read();
      first.setFloat(0, 999);
      assertThat(coordinate.read().getFloat(0)).isEqualTo(-180);
      Array section = coordinate.read(new Section("0:3"));
      section.setFloat(0, 999);
      assertThat(coordinate.read(new Section("0:3")).getFloat(0)).isEqualTo(-180);
    }
  }

  @Test
  public void sectionSliceAndCopiedVariablePreserveCoordinates() throws IOException, InvalidRangeException {
    Variable coordinate = coordinate(8193, 103.829457843, 0.0000011234567);
    Array eager = Array.makeArray(DataType.FLOAT, 8193, 103.829457843, 0.0000011234567);
    Section first = new Section("7:8000:3");
    Section second = new Section("11:37:2");
    Array expected = eager.sectionNoReduce(first.getRanges()).sectionNoReduce(second.getRanges());
    assertThat(coordinate.section(first).read(second).copyTo1DJavaArray()).isEqualTo(expected.copyTo1DJavaArray());
    assertThat(coordinate.slice(0, 77).readScalarFloat()).isEqualTo(eager.getFloat(77));
    assertThat(coordinate.toBuilder().build(Group.builder().build()).read().copyTo1DJavaArray())
        .isEqualTo(eager.copyTo1DJavaArray());
  }

  @Test
  public void openingGribDoesNotMaterializeRegularCoordinates() throws IOException {
    for (String file : new String[] {"cosmo-eu.grib2", "sref.pds2.grib2", "HLYA10.grib2"}) {
      try (NetcdfFile nc = NetcdfFiles.open(TestDir.localTestDataDir + file)) {
        int coordinates = 0;
        for (Variable variable : nc.getVariables()) {
          if (variable.isCoordinateVariable() && variable.getDataType() == DataType.FLOAT) {
            String name = variable.getShortName();
            if (name.equals("x") || name.equals("y") || name.equals("lat") || name.equals("lon") || name.equals("rlat")
                || name.equals("rlon")) {
              assertThat(variable.hasCachedData()).isFalse();
              Array data = variable.read();
              assertThat(data.getShape()).isEqualTo(variable.getShape());
              coordinates++;
            }
          }
        }
        assertThat(coordinates).isEqualTo(2);
      }
    }
  }

  private static Variable coordinate(int length, double start, double increment) {
    return Variable.builder().setName("x").setDataType(DataType.FLOAT).setDimensionsAnonymous(new int[] {length})
        .setProxyReader(new GribCoordinateReader(start, increment)).build(Group.builder().build());
  }
}
