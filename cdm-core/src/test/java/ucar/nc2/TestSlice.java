/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import java.io.IOException;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.nc2.write.NetcdfFormatWriter.Builder;

import static com.google.common.truth.Truth.assertThat;

/** Test writing data and reading slices of it. */
public class TestSlice {

  private static final String DATA_VARIABLE = "data";
  private static final int DIM_T = 10;
  private static final int DIM_ALT = 5;
  private static final int DIM_LAT = 123;
  private static final int DIM_LON = 234;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String filePath;

  @Before
  public void setUp() throws IOException, InvalidRangeException {
    filePath = tempFolder.newFile().getAbsolutePath();

    Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filePath);
    writerb.addDimension("t", DIM_T);
    writerb.addDimension("alt", DIM_ALT);
    writerb.addDimension("lat", DIM_LAT);
    writerb.addDimension("lon", DIM_LON);
    writerb.addVariable(DATA_VARIABLE, ArrayType.FLOAT, "t alt lat lon");

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(DATA_VARIABLE);
      Array<?> data = createData();
      writer.write(v, data.getIndex(), data);
    }
  }

  private Array<?> createData() {
    int[] shape = new int[] {DIM_T, DIM_ALT, DIM_LAT, DIM_LON};
    float[] parray = new float[(int) Arrays.computeSize(shape)];
    int count = 0;
    for (int i = 0; i < DIM_T; i++) {
      for (int j = 0; j < DIM_ALT; j++) {
        for (int k = 0; k < DIM_LAT; k++) {
          for (int l = 0; l < DIM_LON; l++) {
            parray[count++] = i + j;
          }
        }
      }
    }
    return Arrays.factory(ArrayType.FLOAT, shape, parray);
  }

  @Test
  public void testSlice1() throws Exception {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      assertThat(var).isNotNull();
      Variable sliced = var.slice(0, 3);
      sliced.readArray();

      int[] shape = sliced.getShape();
      assertThat(3).isEqualTo(shape.length);
      assertThat(DIM_ALT).isEqualTo(shape[0]);
      assertThat(DIM_LAT).isEqualTo(shape[1]);
      assertThat(DIM_LON).isEqualTo(shape[2]);

      assertThat("alt lat lon").isEqualTo(sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice2() throws Exception {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      assertThat(var).isNotNull();
      Variable sliced = var.slice(1, 3);
      sliced.readArray();

      int[] shape = sliced.getShape();
      assertThat(3).isEqualTo(shape.length);
      assertThat(DIM_T).isEqualTo(shape[0]);
      assertThat(DIM_LAT).isEqualTo(shape[1]);
      assertThat(DIM_LON).isEqualTo(shape[2]);

      assertThat("t lat lon").isEqualTo(sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice3() throws Exception {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      assertThat(var).isNotNull();
      Variable sliced1 = var.slice(0, 3);
      Variable sliced2 = sliced1.slice(0, 3);

      int[] shape = sliced2.getShape();
      assertThat(2).isEqualTo(shape.length);
      assertThat(DIM_LAT).isEqualTo(shape[0]);
      assertThat(DIM_LON).isEqualTo(shape[1]);

      assertThat("lat lon").isEqualTo(sliced2.getDimensionsString());

      ucar.array.Array<?> org = var.readArray(new ucar.array.Section("3,3,:,:"));
      ucar.array.Array<?> data = sliced2.readArray();
      CompareNetcdf2.compareData(DATA_VARIABLE, org, data);
    }
  }
}
